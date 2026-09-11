package com.gymwatch.adapters.driven.wearsync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.gymwatch.core.domain.model.RecentAudiobooks
import com.gymwatch.core.domain.port.RecentAudiobooksRepositoryPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await

/**
 * The recent audiobooks, kept in a Data Layer item that Google Play services
 * syncs between phone and watch.
 *
 * The item is the store itself. Both devices keep a copy, so the watch shows
 * the last list it received even with the phone out of reach, and gets the new
 * one when the phone comes back. That only works between apps with the same
 * package name and signing key: the phone app ships as `com.gymwatch`, signed
 * with the watch's release key.
 *
 * Only the phone writes. Items are matched by path alone, never by a `wear:`
 * URI filter: registered without a host, a listener never fired on the phone,
 * although the item was being written and synced (docs/LESSONS.md #33). A
 * client only ever sees its own app's items, so the path is enough, and the
 * watch finds the phone's copy without knowing the phone's node id.
 */
class DataLayerRecentAudiobooks(context: Context) : RecentAudiobooksRepositoryPort {

    private val client: DataClient = Wearable.getDataClient(context.applicationContext)

    /**
     * What this device wrote. The listener reports changes made by the other
     * device, but on the phone it never reported the phone's own writes: the
     * watch's list moved while the phone's screen stayed put (docs/LESSONS.md #33).
     */
    private val written = MutableSharedFlow<RecentAudiobooks>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * The stored list first, then every change, from either device. The listener
     * goes on before the read, so a change landing in between is not missed; at
     * worst the same list arrives twice.
     */
    override val recent: Flow<RecentAudiobooks> get() = merge(fromDataLayer(), written)

    private fun fromDataLayer(): Flow<RecentAudiobooks> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { events ->
            events
                .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == AudiobookWire.RECENT_PATH }
                .lastOrNull()
                ?.let { trySend(decode(DataMapItem.fromDataItem(it.dataItem).dataMap)) }
        }
        try {
            client.addListener(listener).await()
            send(load())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No Play services, or the Wearable API is unavailable: an empty
            // list, not a crash.
            Log.w(TAG, "Data Layer unavailable", e)
            send(RecentAudiobooks.EMPTY)
        }
        awaitClose { client.removeListener(listener) }
    }

    override suspend fun save(recent: RecentAudiobooks) {
        val columns = AudiobookWire.encode(recent)
        val request = PutDataMapRequest.create(AudiobookWire.RECENT_PATH).apply {
            dataMap.putStringArrayList(AudiobookWire.TITLES, ArrayList(columns.titles))
            dataMap.putStringArrayList(AudiobookWire.AUTHORS, ArrayList(columns.authors))
            dataMap.putLongArray(AudiobookWire.LENGTHS, columns.lengths.toLongArray())
            dataMap.putLongArray(AudiobookWire.POSITIONS, columns.positions.toLongArray())
        }
        // Urgent: the default lets the system hold a change back for up to 30
        // minutes, and the list is looked at right after a book is played.
        client.putDataItem(request.asPutDataRequest().setUrgent()).await()
        written.emit(recent)
    }

    private suspend fun load(): RecentAudiobooks {
        val buffer = client.dataItems.await()
        return try {
            buffer.firstOrNull { it.uri.path == AudiobookWire.RECENT_PATH }
                ?.let { decode(DataMapItem.fromDataItem(it).dataMap) }
                ?: RecentAudiobooks.EMPTY
        } finally {
            buffer.release()
        }
    }

    private fun decode(map: DataMap): RecentAudiobooks = AudiobookWire.decode(
        AudiobookWire.Columns(
            titles = map.getStringArrayList(AudiobookWire.TITLES).orEmpty(),
            authors = map.getStringArrayList(AudiobookWire.AUTHORS).orEmpty(),
            lengths = map.getLongArray(AudiobookWire.LENGTHS)?.toList().orEmpty(),
            positions = map.getLongArray(AudiobookWire.POSITIONS)?.toList().orEmpty(),
        ),
    )

    private companion object {
        const val TAG = "GymWatch"
    }
}
