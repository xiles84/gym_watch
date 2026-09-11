package com.gymwatch.adapters.driven.wearsync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.gymwatch.core.domain.model.PlayOutcome
import com.gymwatch.core.domain.port.AudiobookPlayerPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

/**
 * The watch's side of starting a book: ask the phone, and wait for its answer.
 *
 * A request rather than a one-way message, so the watch can tell "starting" from
 * "the phone app needs notification access". Also not a Data Layer item, which
 * would be replayed later when a phone reconnects: a book should never start
 * itself an hour after the tap.
 */
class DataLayerAudiobookPlayer(context: Context) : AudiobookPlayerPort {

    private val nodes: NodeClient = Wearable.getNodeClient(context.applicationContext)
    private val messages: MessageClient = Wearable.getMessageClient(context.applicationContext)

    override suspend fun play(title: String): PlayOutcome = try {
        withTimeout(ANSWER_WITHIN) {
            val connected = nodes.connectedNodes.await()
            val phone = connected.firstOrNull { it.isNearby } ?: connected.firstOrNull()
            if (phone == null) {
                PlayOutcome.PHONE_UNREACHABLE
            } else {
                val answer = messages
                    .sendRequest(phone.id, AudiobookWire.PLAY_PATH, title.toByteArray(Charsets.UTF_8))
                    .await()
                AudiobookWire.decodeOutcome(answer)
            }
        }
    } catch (e: TimeoutCancellationException) {
        Log.w(TAG, "Phone did not answer a play request in time", e)
        PlayOutcome.PHONE_UNREACHABLE
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Also what a phone without the companion app looks like: nothing is
        // registered there to take the request.
        Log.w(TAG, "Play request failed", e)
        PlayOutcome.PHONE_UNREACHABLE
    }

    private companion object {
        const val TAG = "GymWatch"

        /** Starting Audible when it isn't running takes the phone up to ~10 s. */
        val ANSWER_WITHIN = 20.seconds
    }
}
