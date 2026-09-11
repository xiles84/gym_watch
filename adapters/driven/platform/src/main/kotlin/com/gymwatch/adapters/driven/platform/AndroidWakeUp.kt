package com.gymwatch.adapters.driven.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.gymwatch.core.domain.port.WakeUpPort
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * An exact `ELAPSED_REALTIME_WAKEUP` alarm for zero, and a partial wake lock
 * while the alarm rings.
 *
 * `ELAPSED_REALTIME` is the timeline [AndroidClock] reads, so a mark taken from
 * the clock can be booked here unchanged. The alarm is exact because a rest
 * timer that buzzes a minute late is the bug this exists to fix; `USE_EXACT_ALARM`
 * is granted at install to timer apps, and if it is ever missing the inexact
 * alarm is still far better than none.
 *
 * The callback is held in memory, not in the intent. If the process died, the
 * countdown died with it, so there is nothing for a wake-up to ring.
 */
class AndroidWakeUp(context: Context) : WakeUpPort {

    private val app = context.applicationContext
    private val alarms = app.getSystemService(AlarmManager::class.java)

    override fun wakeAt(at: Duration, onWake: () -> Unit) {
        pending = onWake
        val trigger = at.inWholeMilliseconds
        val operation = operation()
        try {
            if (alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, operation)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, operation)
            }
        } catch (e: SecurityException) {
            // Degrade, never crash: the countdown on screen stays right, only the
            // screen-off buzz goes back to being late.
            Log.w(TAG, "Exact alarm refused; rest alarm may be late with the screen off", e)
        }
    }

    override fun stayAwake() {
        wakeLock(app).acquire(RING_LIMIT.inWholeMilliseconds)
    }

    override fun release() {
        pending = null
        alarms.cancel(operation())
        val lock = wakeLock(app)
        if (lock.isHeld) lock.release()
    }

    private fun operation(): PendingIntent = PendingIntent.getBroadcast(
        app,
        0,
        Intent(app, WakeUpReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal companion object {
        private const val TAG = "GymWatch"

        /**
         * A cap, not a schedule: [release] normally ends it. It only matters if
         * the alarm is left ringing on a watch nobody is wearing.
         */
        val RING_LIMIT = 10.minutes

        /**
         * AlarmManager's own wake lock ends when `onReceive` returns, and the
         * callback does its work on another thread. This bridges the gap until
         * [stayAwake] takes over.
         */
        val WAKE_GRACE = 10.seconds

        @Volatile
        var pending: (() -> Unit)? = null

        private var lock: PowerManager.WakeLock? = null

        /** One lock, not reference-counted: a later acquire only moves its timeout. */
        @Synchronized
        fun wakeLock(context: Context): PowerManager.WakeLock =
            lock ?: context.getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymWatch:rest-alarm")
                .apply { setReferenceCounted(false) }
                .also { lock = it }
    }
}

/** Fired by the alarm booked in [AndroidWakeUp.wakeAt]. */
class WakeUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val onWake = AndroidWakeUp.pending ?: return
        AndroidWakeUp.pending = null
        val lock = AndroidWakeUp.wakeLock(context.applicationContext)
        // Held already means the alarm is ringing; re-acquiring would cut its
        // limit down to the grace period.
        if (!lock.isHeld) lock.acquire(AndroidWakeUp.WAKE_GRACE.inWholeMilliseconds)
        onWake()
    }
}
