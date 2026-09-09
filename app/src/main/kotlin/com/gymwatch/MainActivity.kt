package com.gymwatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.gymwatch.adapters.driving.service.GymSessionService
import com.gymwatch.adapters.driving.ui.GymApp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as GymApplication).container }

    /**
     * Health Services will not start an exercise without these, and the
     * foreground service cannot use the `health` type without one of them
     * granted (docs/LESSONS.md #19). Asked for on first workout, not at launch:
     * the chronometer and counter need nothing, so nothing should be demanded
     * before they can be used.
     */
    private var pendingPermissionResult: ((Boolean) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.any { it } || results.isEmpty()
        pendingPermissionResult?.invoke(granted)
        pendingPermissionResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeTimerState()

        setContent {
            GymApp(
                chronometer = container.chronometer,
                restTimer = container.restTimer,
                counter = container.counter,
                workouts = container.workouts,
                initialPage = intent?.getIntExtra(EXTRA_PAGE, 0) ?: 0,
                onWorkoutStateChanged = ::onWorkoutStateChanged,
                ensurePermissions = ::ensurePermissions,
            )
        }
    }

    private suspend fun ensurePermissions(): Boolean {
        val outstanding = container.permissions.outstanding()
        if (outstanding.isEmpty()) return true
        return suspendCoroutine { continuation ->
            pendingPermissionResult = { granted -> continuation.resume(granted) }
            permissionLauncher.launch(outstanding.toTypedArray())
        }
    }

    private fun onWorkoutStateChanged(running: Boolean) {
        if (running) {
            GymSessionService.start(
                context = this,
                title = "Workout",
                status = "Recording",
                launchIntent = selfIntent(),
                health = true,
            )
        } else {
            GymSessionService.stop(this)
        }
    }

    /**
     * The foreground service runs exactly while a timer is running, so the watch
     * face indicator appears and disappears on its own. Driven from the use case
     * state rather than from button handlers, so it cannot drift out of sync
     * with what is actually happening.
     */
    private fun observeTimerState() {
        lifecycleScope.launch {
            combine(
                container.chronometer.state,
                container.restTimer.state,
                container.workouts.snapshot,
            ) { chrono, rest, workout -> Triple(chrono.isRunning, rest.isRunning, workout != null) }
                .collect { (chronoRunning, restRunning, workoutRunning) ->
                    when {
                        // A workout owns the service while it runs; it needs the
                        // health service type, which the timers must not claim.
                        workoutRunning -> Unit

                        restRunning -> GymSessionService.start(
                            this@MainActivity,
                            title = "Rest",
                            status = "Resting",
                            launchIntent = selfIntent(),
                        )

                        chronoRunning -> GymSessionService.start(
                            this@MainActivity,
                            title = "Gym Watch",
                            status = "Chronometer running",
                            launchIntent = selfIntent(),
                        )

                        else -> GymSessionService.stop(this@MainActivity)
                    }
                }
        }
    }

    companion object {
        /**
         * Opens the app on a given page. Used by the Ongoing Activity indicator
         * and the tile, so a tap lands where the user expects instead of always
         * on the chronometer.
         */
        const val EXTRA_PAGE = "page"
    }

    // No FLAG_ACTIVITY_NEW_TASK / CLEAR_TOP: they break Recents on Wear OS, and
    // a PendingIntent to an Activity does not need them.
    private fun selfIntent() = Intent(this, MainActivity::class.java)
}
