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

    /** Fraction still to run, 1.0 down to 0.0 — for the progress ring. */
    fun progressAt(now: Duration): Float =
        if (duration == Duration.ZERO) 0f
        else (remainingAt(now) / duration).toFloat().coerceIn(0f, 1f)

    fun start(now: Duration): RestTimer = copy(startMark = now)

    fun cancel(): RestTimer = copy(startMark = null)

    fun withDuration(new: Duration): RestTimer =
        copy(duration = new.coerceIn(MIN, MAX), startMark = null)

    /** Step the configured rest length, e.g. from the rotary bezel. */
    fun adjustBy(delta: Duration): RestTimer = withDuration(duration + delta)

    companion object {
        val MIN = 5.seconds
        val MAX = 15.minutes
        val DEFAULT = 90.seconds
        val STEP = 15.seconds
    }
}
