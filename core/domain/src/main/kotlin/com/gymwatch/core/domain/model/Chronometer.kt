package com.gymwatch.core.domain.model

import kotlin.time.Duration

/**
 * A count-up stopwatch, modelled as a *start mark plus banked time* rather than
 * a tick counter.
 *
 * This is the load-bearing decision in the whole app: because elapsed time is
 * derived from the clock on every read, the value stays correct across screen
 * off, doze, and process death. A tick loop would silently drift or stop.
 */
data class Chronometer(
    /** Clock reading when the current run began; null when not running. */
    val startMark: Duration? = null,
    /** Time banked by previous runs, before the current one. */
    val accumulated: Duration = Duration.ZERO,
    /** Total elapsed time at each lap mark, oldest first. */
    val laps: List<Duration> = emptyList(),
) {
    val isRunning: Boolean get() = startMark != null

    val isReset: Boolean
        get() = startMark == null && accumulated == Duration.ZERO && laps.isEmpty()

    /**
     * Only a running chronometer asks before resetting — that is a set being
     * timed. A paused one was stopped on purpose.
     */
    val needsResetConfirmation: Boolean get() = isRunning

    /** Total elapsed time as of [now]. */
    fun elapsedAt(now: Duration): Duration =
        accumulated + (startMark?.let { now - it } ?: Duration.ZERO)

    /** Time spent in the current lap as of [now]. */
    fun currentLapAt(now: Duration): Duration =
        elapsedAt(now) - (laps.lastOrNull() ?: Duration.ZERO)

    /** Starting an already-running chronometer is a no-op, not an error. */
    fun start(now: Duration): Chronometer =
        if (isRunning) this else copy(startMark = now)

    /** Banks the current run. Pausing a paused chronometer is a no-op. */
    fun pause(now: Duration): Chronometer =
        if (!isRunning) this else copy(startMark = null, accumulated = elapsedAt(now))

    fun toggle(now: Duration): Chronometer = if (isRunning) pause(now) else start(now)

    /** Marks a lap. Ignored when not running — an idle lap means nothing. */
    fun lap(now: Duration): Chronometer =
        if (!isRunning) this else copy(laps = laps + elapsedAt(now))

    fun reset(): Chronometer = Chronometer()

    companion object {
        val Idle = Chronometer()
    }
}
