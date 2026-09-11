package com.gymwatch.adapters.driving.ui

import kotlin.time.Duration

/** `12:04`, or `1:02:04` once it passes an hour. Fixed-width so it stops jittering. */
internal fun Duration.asClock(): String {
    val total = inWholeSeconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** `5h 19m`, or `42m` under an hour, for an audiobook's time left. */
internal fun Duration.asHoursAndMinutes(): String {
    val h = inWholeHours.coerceAtLeast(0)
    val m = (inWholeMinutes % 60).coerceAtLeast(0)
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** `1:30` for a configured rest length. */
internal fun Duration.asRestLabel(): String = asClock()
