package com.gymwatch.adapters.driven.platform

import android.os.SystemClock
import com.gymwatch.core.domain.port.ClockPort
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The only clock the app uses.
 *
 * `elapsedRealtime()` counts from boot and includes deep sleep, and cannot be
 * moved by the user or by network time sync. `System.currentTimeMillis()` can,
 * which is why the core forbids it (see docs/LESSONS.md).
 */
object AndroidClock : ClockPort {
    override fun elapsed(): Duration = SystemClock.elapsedRealtime().milliseconds
}
