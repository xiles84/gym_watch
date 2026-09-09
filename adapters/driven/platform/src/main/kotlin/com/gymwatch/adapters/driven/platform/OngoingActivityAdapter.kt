package com.gymwatch.adapters.driven.platform

import android.content.Context
import android.content.Intent
import com.gymwatch.core.domain.port.OngoingActivityPort

/**
 * Updates the watch-face indicator while something is running.
 *
 * [launchIntent] is what a tap on the indicator opens — supplied by the app
 * module, because this adapter must not know which Activity exists.
 */
class OngoingActivityAdapter(
    private val context: Context,
    private val notifications: GymNotifications,
    private val launchIntent: () -> Intent,
) : OngoingActivityPort {

    override fun show(title: String, status: String) {
        notifications.ensureChannel()
        notifications.post(notifications.build(title, status, launchIntent()))
    }

    override fun clear() {
        notifications.cancel()
    }
}
