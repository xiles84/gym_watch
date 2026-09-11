package com.gymwatch.adapters.driven.audible

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.PlayOutcome
import com.gymwatch.core.domain.port.AudiobookPlayerPort
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Starts a book in Audible on the phone, by searching its title on Audible's
 * playback session.
 *
 * This is the one route Audible leaves open (docs/LESSONS.md #32):
 * - Browsing is refused to apps it doesn't trust by signature.
 * - Its `MEDIA_PLAY_FROM_SEARCH` activity only opens the app.
 * - `playFromSearch` on its session switches to the book and resumes it where
 *   it was left, but takes 15–20 seconds. Part of a title is enough.
 *
 * The exception is the book already loaded: searching for it errors, so it
 * gets a plain play instead.
 */
class AudibleSessionPlayer(
    context: Context,
    private val session: AudibleSession,
) : AudiobookPlayerPort {

    private val appContext = context.applicationContext

    override suspend fun play(title: String): PlayOutcome {
        if (!session.hasAccess()) return PlayOutcome.PHONE_NEEDS_ACCESS
        val controller = session.controller() ?: wakeAudible() ?: return PlayOutcome.PLAYER_UNAVAILABLE
        // Searching for the book that is already loaded puts Audible's session
        // into an error state and plays nothing, so that one is resumed instead.
        val loaded = AudibleSession.nowPlaying(controller)?.book
        if (loaded != null && loaded.isSameBookAs(Audiobook(title))) {
            controller.transportControls.play()
        } else {
            controller.transportControls.playFromSearch(title, Bundle.EMPTY)
        }
        return PlayOutcome.STARTING
    }

    /**
     * Audible has no session while it isn't running. A play key sent to its
     * media button receiver starts it, the same way a headset button resumes
     * the last player, and the session appears within a few seconds. The book
     * it resumes is replaced by the search straight after.
     */
    private suspend fun wakeAudible(): MediaController? {
        val probe = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(AudibleSession.AUDIBLE_PACKAGE)
        val receiver = appContext.packageManager.queryBroadcastReceivers(probe, 0).firstOrNull()?.activityInfo
            ?: return null.also { Log.w(TAG, "Audible has no media button receiver") }
        val component = ComponentName(receiver.packageName, receiver.name)

        for (action in listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP)) {
            appContext.sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setComponent(component)
                    .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(action, KeyEvent.KEYCODE_MEDIA_PLAY)),
            )
        }

        repeat(WAKE_ATTEMPTS) {
            delay(WAKE_POLL)
            session.controller()?.let { return it }
        }
        Log.w(TAG, "Audible did not start a session")
        return null
    }

    private companion object {
        const val TAG = "GymWatch"
        val WAKE_POLL = 500.milliseconds
        const val WAKE_ATTEMPTS = 20
    }
}
