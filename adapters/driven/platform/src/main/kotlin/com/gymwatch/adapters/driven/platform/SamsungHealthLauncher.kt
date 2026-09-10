package com.gymwatch.adapters.driven.platform

import android.content.Context
import android.content.Intent
import android.util.Log
import com.gymwatch.core.domain.port.CompanionHealthAppPort

/**
 * Opens Samsung Health on the watch.
 *
 * `getLaunchIntentForPackage` is the *only* documented way in. Community
 * attempts at `health://` and similar URI schemes do not work, and there is no
 * supported deep link to a specific exercise — so this lands on Samsung
 * Health's own home screen and the user picks the workout there
 * (docs/LESSONS.md #2).
 *
 * That is a real limitation, not an oversight: Samsung Health has no
 * third-party write API, so it has to start the exercise itself for the workout
 * to end up in its history.
 */
class SamsungHealthLauncher(private val context: Context) : CompanionHealthAppPort {

    override suspend fun isAvailable(): Boolean = launchIntent() != null

    override suspend fun open(): Boolean {
        // NEW_TASK is required here and only here: this is a real startActivity
        // from a non-Activity context, not a PendingIntent target (lesson #17).
        val intent = launchIntent()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not open Samsung Health", it) }
            .isSuccess
    }

    private fun launchIntent(): Intent? =
        runCatching { context.packageManager.getLaunchIntentForPackage(PACKAGE) }.getOrNull()

    private companion object {
        const val TAG = "GymWatch"

        /** Samsung Health on the *watch*, not the phone package. */
        const val PACKAGE = "com.samsung.android.wear.shealth"
    }
}
