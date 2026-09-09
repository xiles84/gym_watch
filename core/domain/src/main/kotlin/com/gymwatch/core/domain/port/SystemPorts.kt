package com.gymwatch.core.domain.port

import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.HealthPermission

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

interface PermissionsPort {
    suspend fun granted(permission: HealthPermission): Boolean
    suspend fun requestAll(): Boolean
}
