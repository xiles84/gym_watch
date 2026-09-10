package com.gymwatch.adapters.driven.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Builds the one notification this app posts.
 *
 * It serves two purposes at once, which is why it lives in a single place:
 * it is the foreground service's required notification, and it carries the
 * Ongoing Activity that puts a tappable indicator on the watch face while a
 * timer or workout is running. A watch face cannot read app state — this is the
 * supported way to surface it there.
 */
class GymNotifications(private val context: Context) {

    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gym session",
            NotificationManager.IMPORTANCE_LOW,   // no sound; the haptics do the talking
        ).apply {
            description = "Shows while a timer or workout is running"
            setShowBadge(false)
        }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun build(title: String, status: String, touchIntent: Intent): Notification {
        val pending = PendingIntent.getActivity(
            context,
            0,
            touchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(status)
            .setContentIntent(pending)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        OngoingActivity.Builder(context, NOTIFICATION_ID, builder)
            .setStaticIcon(android.R.drawable.ic_menu_recent_history)
            .setTouchIntent(pending)
            .setStatus(Status.forPart(Status.TextPart(status)))
            .build()
            .apply(context)

        return builder.build()
    }

    @Suppress("MissingPermission")
    fun post(notification: Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "gym_session"
        const val NOTIFICATION_ID = 1001
    }
}
