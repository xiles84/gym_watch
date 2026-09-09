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
 * Keeps the process alive while a timer or workout is running.
 *
 * The foreground service *type* is chosen per session, and that matters:
 * `health` requires one of ACTIVITY_RECOGNITION / READ_HEART_RATE to be
 * **granted at runtime**, and throws SecurityException otherwise. A chronometer
 * has no business claiming it. See docs/LESSONS.md #19.
 */
class GymSessionService : Service() {

    private val notifications by lazy { GymNotifications(this) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Gym Watch"
        val status = intent?.getStringExtra(EXTRA_STATUS) ?: "Session running"
        val health = intent?.getBooleanExtra(EXTRA_HEALTH, false) ?: false
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
                val type = if (health) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                }
                startForeground(GymNotifications.NOTIFICATION_ID, notification, type)
            } else {
                startForeground(GymNotifications.NOTIFICATION_ID, notification)
            }
            START_NOT_STICKY
        } catch (e: Exception) {
            Log.w(TAG, "Foreground service refused (health=$health); continuing without it", e)
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
        private const val EXTRA_HEALTH = "health"
        private const val EXTRA_LAUNCH_INTENT = "launch"

        /**
         * @param health true only while a Health Services exercise is running.
         *   Requires ACTIVITY_RECOGNITION or READ_HEART_RATE to be granted.
         */
        fun start(
            context: Context,
            title: String,
            status: String,
            launchIntent: Intent,
            health: Boolean = false,
        ) {
            val intent = Intent(context, GymSessionService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_STATUS, status)
                .putExtra(EXTRA_HEALTH, health)
                .putExtra(EXTRA_LAUNCH_INTENT, launchIntent)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, GymSessionService::class.java)) }
        }
    }
}
