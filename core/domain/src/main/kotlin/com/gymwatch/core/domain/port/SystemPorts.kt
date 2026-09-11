package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Haptic
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
