package com.gymwatch.adapters.driven.audible

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.gymwatch.core.domain.model.NowPlaying
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * What Audible is playing, as it changes.
 *
 * Emits null whenever Audible has no session. Only collect this while the
 * notification listener is connected: before then the system refuses to list
 * sessions at all.
 */
class AudibleSessionWatcher(context: Context, private val listener: ComponentName) {

    private val manager = context.applicationContext.getSystemService(MediaSessionManager::class.java)

    fun nowPlaying(): Flow<NowPlaying?> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        var current: MediaController? = null

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                current?.let { trySend(AudibleSession.nowPlaying(it)) }
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                current?.let { trySend(AudibleSession.nowPlaying(it)) }
            }

            override fun onSessionDestroyed() {
                current = null
                trySend(null)
            }
        }

        fun attach(controllers: List<MediaController>?) {
            val audible = controllers?.firstOrNull { it.packageName == AudibleSession.AUDIBLE_PACKAGE }
            if (audible?.sessionToken == current?.sessionToken) return
            current?.unregisterCallback(callback)
            current = audible
            audible?.registerCallback(callback, handler)
            trySend(audible?.let(AudibleSession::nowPlaying))
        }

        val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }

        try {
            manager.addOnActiveSessionsChangedListener(sessionsChanged, listener, handler)
            attach(manager.getActiveSessions(listener))
        } catch (e: SecurityException) {
            Log.w(TAG, "No notification access; cannot see Audible", e)
            trySend(null)
        }

        awaitClose {
            manager.removeOnActiveSessionsChangedListener(sessionsChanged)
            current?.unregisterCallback(callback)
        }
    }

    private companion object {
        const val TAG = "GymWatch"
    }
}
