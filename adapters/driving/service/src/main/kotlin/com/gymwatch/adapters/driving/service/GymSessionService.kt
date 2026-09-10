package com.gymwatch.adapters.driving.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.gymwatch.adapters.driven.platform.GymNotifications

/**
 * Keeps the process alive while a timer is running.
 *
 * The type is always `specialUse`. It used to be chosen per session because a
 * Health Services exercise needs `health`, which in turn needs a runtime
 * permission granted or `startForeground` throws (docs/LESSONS.md #19). The app
 * no longer runs exercises — Samsung Health does — so the health type, and the
 * permissions it dragged in, are gone.
 *
 * `specialUse` is the honest description of what this is: a chronometer and a
 * rest timer, which are not health tracking.
 */
class GymSessionService : Service() {

    private val notifications by lazy { GymNotifications(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Gym Watch"
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: "Session running"
        val launch = intent?.getParcelableExtra<Intent>(EXTRA_LAUNCH_INTENT)
            ?: packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent()

        notifications.ensureChannel()
        val notification = notifications.build(title, status, launch)

        // Degrade, never crash. If the platform refuses the foreground service —
        // a permission not yet granted, a background-start restriction — the
        // timers still keep perfect time, because they derive from the clock
        // rather than from anything this service does. Losing the notification
        // is a real but survivable loss; taking the app down with it is not.
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Foreground service types only exist from API 34.
                startForeground(
                    GymNotifications.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(GymNotifications.NOTIFICATION_ID, notification)
            }
            START_NOT_STICKY
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service refused; continuing without it", e)
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        notifications.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GymWatch"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_STATUS = "status"
        private const val EXTRA_LAUNCH_INTENT = "launch"

        fun start(context: Context, title: String, status: String, launchIntent: Intent) {
            val intent = Intent(context, GymSessionService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_STATUS, status)
                .putExtra(EXTRA_LAUNCH_INTENT, launchIntent)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, GymSessionService::class.java)) }
        }
    }
}
