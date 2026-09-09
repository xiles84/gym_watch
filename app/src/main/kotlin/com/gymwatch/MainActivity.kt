package com.gymwatch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.gymwatch.adapters.driving.service.GymSessionService
import com.gymwatch.adapters.driving.ui.GymApp
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container by lazy { (application as GymApplication).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        observeSessionState()

        setContent {
            GymApp(
                chronometer = container.chronometer,
                restTimer = container.restTimer,
                counter = container.counter,
                workouts = container.workouts,
                onStartWorkout = { /* phase 4: Health Services */ },
            )
        }
    }

    /**
     * The foreground service runs exactly while something is running, so the
     * watch face indicator appears and disappears on its own. Driven from the
     * use cases rather than from button handlers, so it cannot drift out of sync
     * with the actual state.
     */
    private fun observeSessionState() {
        lifecycleScope.launch {
            combine(
                container.chronometer.state,
                container.restTimer.state,
            ) { chrono, rest -> chrono.isRunning to rest.isRunning }
                .collect { (chronoRunning, restRunning) ->
                    when {
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

    // No FLAG_ACTIVITY_NEW_TASK / CLEAR_TOP: they break Recents on Wear OS, and
    // a PendingIntent to an Activity does not need them.
    private fun selfIntent() = Intent(this, MainActivity::class.java)
}
