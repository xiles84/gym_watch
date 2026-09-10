package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.Haptic

fun interface HapticsPort {
    fun play(haptic: Haptic)
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
 * No package name appears here on purpose. Which app this is, and the fact that
 * only `getLaunchIntentForPackage` works to reach it, are adapter concerns.
 */
interface CompanionHealthAppPort {
    suspend fun isAvailable(): Boolean

    /** Returns false if the app is missing or refused to launch. */
    suspend fun open(): Boolean
}
