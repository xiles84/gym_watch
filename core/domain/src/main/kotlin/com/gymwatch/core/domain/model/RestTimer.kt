package com.gymwatch.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A countdown for rest between sets. Same start-mark model as [Chronometer],
 * for the same reason: it must stay correct while the screen is off.
 */
data class RestTimer(
    val duration: Duration = DEFAULT,
    /** Clock reading when the countdown began; null when idle. */
    val startMark: Duration? = null,
) {
    val isRunning: Boolean get() = startMark != null

    /** Never negative — it clamps at zero once expired. */
    fun remainingAt(now: Duration): Duration {
        val start = startMark ?: return duration
        val left = duration - (now - start)
        return if (left.isNegative()) Duration.ZERO else left
    }

    fun hasExpiredAt(now: Duration): Boolean = isRunning && remainingAt(now) == Duration.ZERO

    /**
     * Ringing: the countdown reached zero and has not been reset. The start mark
     * is kept until then on purpose, so the alarm is derived from the clock like
     * the countdown and survives the screen going off.
     */
    fun isAlarmingAt(now: Duration): Boolean = hasExpiredAt(now)

    /** Asks only mid-countdown. At zero, resetting is how the alarm is silenced. */
    fun needsResetConfirmationAt(now: Duration): Boolean = isRunning && !hasExpiredAt(now)

    /** Fraction still to run, 1.0 down to 0.0 — for the progress ring. */
    fun progressAt(now: Duration): Float =
        if (duration == Duration.ZERO) 0f
        else (remainingAt(now) / duration).toFloat().coerceIn(0f, 1f)

    fun start(now: Duration): RestTimer = copy(startMark = now)

    fun cancel(): RestTimer = copy(startMark = null)

    fun withDuration(new: Duration): RestTimer =
        copy(duration = new.coerceIn(MIN, MAX), startMark = null)

    companion object {
        val MIN = 5.seconds
        val MAX = 15.minutes
        val DEFAULT = 90.seconds
    }
}
