package com.gymwatch.adapters.driven.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.port.CompanionHealthAppPort

/**
 * Opens Samsung Health on the watch — on its home screen, or straight on one
 * exercise's start screen.
 *
 * The direct route is **undocumented**. It is exactly the intent Samsung
 * Health's own watch-face complication fires: its exported `ExerciseActivity`,
 * action `START_WORKOUT`, and the exercise named in the `exercise.type` extra.
 * Community attempts at `health://` URIs never worked; reading the APK did
 * (docs/LESSONS.md #28). It lands on the start screen and stops there — the
 * user presses start, and Samsung Health records the workout, which nothing
 * else can do (#2).
 *
 * Because an update can remove the route, [startWorkout] reports false when the
 * Activity no longer resolves, and the use case falls back to [open].
 */
class SamsungHealthLauncher(private val context: Context) : CompanionHealthAppPort {

    override suspend fun isAvailable(): Boolean = launchIntent() != null

    override suspend fun open(): Boolean {
        // NEW_TASK is required here and only here: this is a real startActivity
        // from a non-Activity context, not a PendingIntent target (lesson #17).
        val intent = launchIntent()?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return launch(intent, "Samsung Health")
    }

    override suspend fun startWorkout(kind: ExerciseKind): Boolean {
        val intent = Intent(ACTION_START_WORKOUT)
            .setClassName(SAMSUNG_HEALTH_PACKAGE, EXERCISE_ACTIVITY)
            .putExtra(EXTRA_EXERCISE_TYPE, kind.samsung().type)
            // The complication's own flags: a fresh task on that exercise's start
            // screen, not whatever Samsung Health was last showing.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val resolves = runCatching {
            context.packageManager.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        }.getOrNull() != null
        if (!resolves) return false

        return launch(intent, "Samsung Health on ${kind.name}")
    }

    private fun launch(intent: Intent, what: String): Boolean =
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not open $what", it) }
            .isSuccess

    private fun launchIntent(): Intent? =
        runCatching { context.packageManager.getLaunchIntentForPackage(SAMSUNG_HEALTH_PACKAGE) }
            .getOrNull()

    private companion object {
        const val TAG = "GymWatch"

        const val ACTION_START_WORKOUT =
            "com.samsung.android.wear.shealth.intent.action.START_WORKOUT"
        const val EXERCISE_ACTIVITY =
            "com.samsung.android.wear.shealth.app.exercise.view.ExerciseActivity"
        const val EXTRA_EXERCISE_TYPE = "exercise.type"
    }
}
