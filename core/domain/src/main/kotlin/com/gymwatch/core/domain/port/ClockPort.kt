package com.gymwatch.core.domain.port

import kotlin.time.Duration

/**
 * A monotonic time source.
 *
 * MUST be backed by something that cannot jump — on Android that is
 * `SystemClock.elapsedRealtime()`, never `System.currentTimeMillis()`.
 * A wall clock moves when the user changes the time or the network syncs it,
 * which would corrupt a running chronometer.
 *
 * The origin is arbitrary and meaningless; only differences matter.
 */
fun interface ClockPort {
    fun elapsed(): Duration
}
