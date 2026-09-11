package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.MediaApp
import com.gymwatch.core.domain.model.NowPlaying
import com.gymwatch.core.domain.model.PlayOutcome
import kotlin.time.Duration

fun interface HapticsPort {
    fun play(haptic: Haptic)
}

/**
 * Gets the rest alarm out of deep sleep on time.
 *
 * A coroutine `delay` runs on a clock that stops while the CPU is suspended, and
 * nothing wakes the CPU for it — so with the screen off, a countdown's zero came
 * whenever something else happened to wake the watch (docs/LESSONS.md #31). The
 * countdown itself was never wrong; only the buzz was late.
 */
interface WakeUpPort {
    /**
     * Wakes the device at [at], on [ClockPort]'s timeline, and calls [onWake].
     * Replaces any earlier request. The device stays awake long enough for
     * [onWake] to act.
     */
    fun wakeAt(at: Duration, onWake: () -> Unit)

    /** Keeps the CPU running until [release] — while the alarm rings, so its repeats keep time. */
    fun stayAwake()

    /** Drops a pending [wakeAt] and lets the device sleep again. */
    fun release()
}

/**
 * The system indicator that appears on the watch face while something of ours is
 * running, giving one tap back into the app. This is the only supported way for
 * watch-face-adjacent UI to reflect app state — a watch face cannot read it.
 */
interface OngoingActivityPort {
    fun show(title: String, status: String)
    fun clear()
}

/**
 * The watch's own health app — on a Galaxy Watch, Samsung Health.
 *
 * It owns the workout record, because nothing else can: Health Services streams
 * live metrics but persists nothing, Health Connect does not run on Wear OS, and
 * Samsung Health has no third-party write API. See docs/LESSONS.md #2.
 *
 * No package name or intent appears here on purpose. Which app this is, and the
 * undocumented route into a specific exercise, are adapter concerns (#28).
 */
interface CompanionHealthAppPort {
    suspend fun isAvailable(): Boolean

    /** Opens the app's home screen. Returns false if it is missing or refused. */
    suspend fun open(): Boolean

    /**
     * Opens the app on [kind]'s start screen, ready for the user to press start.
     * Returns false when that route is unavailable, so the caller can fall back
     * to [open].
     */
    suspend fun startWorkout(kind: ExerciseKind): Boolean
}

/**
 * Starts an audiobook on the phone, by title.
 *
 * On the watch this is a request carried to the phone; on the phone it is the
 * command to the audiobook player itself. A title is all there is to go on:
 * Audible accepts a search on its playback session from another app, but will
 * not let one browse its library (docs/LESSONS.md #32).
 */
fun interface AudiobookPlayerPort {
    suspend fun play(title: String): PlayOutcome
}

/** What the phone's audiobook player has loaded right now, playing or paused. */
fun interface NowPlayingPort {
    /** Null when nothing is loaded, the player isn't running, or the app can't see it. */
    suspend fun current(): NowPlaying?
}

/** Other apps on the watch. Which packages they are is the adapter's business. */
interface MediaAppsPort {
    suspend fun isInstalled(app: MediaApp): Boolean

    /** Returns false if the app is missing or refused to open. */
    suspend fun open(app: MediaApp): Boolean
}
