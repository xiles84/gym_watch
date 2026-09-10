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

/** `1:30` for a configured rest length. */
internal fun Duration.asRestLabel(): String = asClock()
