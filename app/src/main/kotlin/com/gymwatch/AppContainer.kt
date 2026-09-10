package com.gymwatch

import android.content.Context
import android.content.Intent
import com.gymwatch.adapters.driven.persistence.PersistenceAdapters
import com.gymwatch.adapters.driven.platform.AndroidClock
import com.gymwatch.adapters.driven.platform.AndroidHaptics
import com.gymwatch.adapters.driven.platform.GymNotifications
import com.gymwatch.adapters.driven.platform.OngoingActivityAdapter
import com.gymwatch.adapters.driven.platform.SamsungHealthLauncher
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.ProfilesUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.ScreenLayoutUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * The composition root — the one place that knows both the core and every
 * adapter. Nothing else in the app wires dependencies.
 *
 * Manual constructor injection rather than Hilt: annotation processing would
 * pull Android into modules deliberately kept pure, and the graph fits on a
 * screen.
 */
class AppContainer(private val context: Context) {

    /**
     * Application-scoped on purpose. The use cases hold the running chronometer
     * and timer state, so they must outlive any Activity — rotating the wrist
     * away and back must not reset a set.
     */
    private val scope = CoroutineScope(SupervisorJob())

    private val haptics = AndroidHaptics(context)
    private val notifications = GymNotifications(context)

    val ongoingActivity = OngoingActivityAdapter(
        context = context,
        notifications = notifications,
        launchIntent = {
            Intent(context, MainActivity::class.java)
        },
    )

    private val persistence = PersistenceAdapters(context)

    /** Samsung Health owns the workout record; we only open it (LESSONS.md #2). */
    private val healthApp = SamsungHealthLauncher(context)

    val chronometer = ChronometerUseCase(AndroidClock, haptics)

    val restTimer = RestTimerUseCase(
        clock = AndroidClock,
        profiles = persistence.profiles,
        haptics = haptics,
        scope = scope,
    )

    val counter = CounterUseCase(persistence.counters, haptics, scope)

    val profiles = ProfilesUseCase(
        repository = persistence.profiles,
        healthApp = healthApp,
        haptics = haptics,
        scope = scope,
    )

    val screenLayout = ScreenLayoutUseCase(
        repository = persistence.screenLayout,
        haptics = haptics,
        scope = scope,
    )
}
