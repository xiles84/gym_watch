package com.gymwatch.adapters.driven.audible

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.NowPlaying
import com.gymwatch.core.domain.port.NowPlayingPort
import kotlin.time.Duration.Companion.milliseconds

/**
 * Finds Audible's playback session and reads it.
 *
 * Another app can only see media sessions while it holds notification access:
 * [listener] is the app's own `NotificationListenerService`, and the user has
 * to switch it on in Settings. Audible checks the caller when another app tries
 * to browse, but not when one controls its session (docs/LESSONS.md #32).
 */
class AudibleSession(context: Context, private val listener: ComponentName) : NowPlayingPort {

    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(MediaSessionManager::class.java)

    fun hasAccess(): Boolean =
        appContext.packageName in NotificationManagerCompat.getEnabledListenerPackages(appContext)

    /** Null when Audible isn't running, or when access was revoked. */
    fun controller(): MediaController? =
        runCatching { manager.getActiveSessions(listener) }
            .getOrNull()
            ?.firstOrNull { it.packageName == AUDIBLE_PACKAGE }

    override suspend fun current(): NowPlaying? = controller()?.let(::nowPlaying)

    companion object {
        const val AUDIBLE_PACKAGE = "com.audible.application"

        /**
         * What Audible's session says is loaded, or null when nothing is.
         *
         * Audible's metadata puts the book in TITLE, the chapter in ARTIST and
         * the authors in ALBUM, as read from its session with Google's Media
         * Controller Test app. DURATION and the position cover the whole book,
         * not the chapter.
         */
        fun nowPlaying(controller: MediaController): NowPlaying? {
            val metadata = controller.metadata ?: return null
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata.description.title?.toString()
            if (title.isNullOrBlank()) return null

            val state = controller.playbackState
            val playing = state?.state == PlaybackState.STATE_PLAYING
            val length = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).takeIf { it > 0 }
            val position = state?.let {
                positionNow(
                    reported = it.position,
                    reportedAt = it.lastPositionUpdateTime,
                    speed = it.playbackSpeed,
                    playing = playing,
                    now = SystemClock.elapsedRealtime(),
                )
            }

            return NowPlaying(
                book = Audiobook(
                    title = title.trim(),
                    author = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty().trim(),
                    length = length?.milliseconds,
                    position = position?.takeIf { it >= 0 }?.milliseconds,
                ),
                isPlaying = playing,
            )
        }

        /**
         * A session reports its position only when something changes, stamped
         * with the time it did. While playing, the real position has moved on
         * since then, at the playback speed.
         */
        internal fun positionNow(reported: Long, reportedAt: Long, speed: Float, playing: Boolean, now: Long): Long {
            if (reported < 0) return -1
            if (!playing || reportedAt <= 0 || now <= reportedAt) return reported
            return reported + ((now - reportedAt) * speed).toLong()
        }
    }
}
