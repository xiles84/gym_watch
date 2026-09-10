package com.gymwatch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gymwatch.adapters.driving.service.GymSessionService
import com.gymwatch.adapters.driving.ui.GymApp
import com.gymwatch.adapters.driving.ui.WorkoutIcons
import com.gymwatch.core.domain.model.AppScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as GymApplication).container }

    /**
     * The only runtime permission left.
     *
     * Health permissions are gone with the exercise tracking that needed them —
     * Samsung Health records workouts now. POST_NOTIFICATIONS stays because the
     * foreground service's notification *is* the watch-face Ongoing Activity
     * indicator, and without the grant it fails silently inside
     * `GymNotifications.post`, so the indicator would simply never appear.
     */
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Declined is survivable: only the indicator is lost, never a timer. */ }

    /**
     * The screen a caller asked for, held as state rather than read once.
     *
     * Android delivers a new intent to the *running* Activity instead of
     * recreating it, so reading this only in [onCreate] would make a tap on the
     * watch-face indicator do nothing whenever the app was already open.
     */
    private var pendingScreen by mutableStateOf<AppScreen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pendingScreen = screenFrom(intent)
        requestNotificationPermissionIfNeeded()
        observeTimerState()

        setContent {
            GymApp(
                chronometer = container.chronometer,
                restTimer = container.restTimer,
                counter = container.counter,
                workouts = container.workouts,
                workoutIcons = WorkoutIcons(container.workoutIcons::bitmap),
                screenLayout = container.screenLayout,
                skins = container.skins,
                requestedScreen = pendingScreen,
                onScreenHandled = { pendingScreen = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingScreen = screenFrom(intent)
    }

    /**
     * Carries a screen *name*, not a page index: once screens can be reordered
     * and hidden, index 2 means nothing stable. The name is resolved against the
     * current layout, and an unknown or hidden one falls back to the first page.
     */
    private fun screenFrom(intent: Intent?): AppScreen? =
        AppScreen.of(intent?.getStringExtra(EXTRA_SCREEN))

    private fun requestNotificationPermissionIfNeeded() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (granted != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * The foreground service runs exactly while a timer is running, so the watch
     * face indicator appears and disappears on its own. Driven from the use case
     * state rather than from button handlers, so it cannot drift out of sync
     * with what is actually happening.
     *
     * The rest alarm counts as running: it keeps the process alive to buzz, and
     * the indicator says so.
     */
    private fun observeTimerState() {
        lifecycleScope.launch {
            combine(
                container.chronometer.state,
                container.restTimer.state,
                container.restTimer.alarming,
            ) { chrono, rest, alarming -> Triple(chrono.isRunning, rest.isRunning, alarming) }
                .collect { (chronoRunning, restRunning, restAlarming) ->
                    when {
                        restAlarming -> GymSessionService.start(
                            this@MainActivity,
                            title = "Rest",
                            status = "Rest over",
                            launchIntent = selfIntent(),
                        )

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
         * Opens the app on a given screen, by [AppScreen] name. Used by the
         * Ongoing Activity indicator and by any future tile, so a tap lands
         * where the user expects (docs/LESSONS.md #23).
         */
        const val EXTRA_SCREEN = "screen"
    }

    // No FLAG_ACTIVITY_NEW_TASK / CLEAR_TOP: they break Recents on Wear OS, and
    // a PendingIntent to an Activity does not need them.
    private fun selfIntent() = Intent(this, MainActivity::class.java)
}
