package com.gymwatch.adapters.driving.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

/**
 * Drives redraws while something is running.
 *
 * This is a *display* concern only. The timers themselves derive their value
 * from the clock and stay correct whether or not this is ticking, so a missed
 * frame or a sleeping screen costs nothing. Never let this become the source of
 * truth (docs/LESSONS.md and ARCHITECTURE.md).
 */
@Composable
internal fun rememberTick(active: Boolean, periodMs: Long = 100L): State<Long> {
    val tick = remember { mutableLongStateOf(0L) }
    LaunchedEffect(active, periodMs) {
        while (active) {
            delay(periodMs)
            tick.longValue++
        }
    }
    return tick
}
