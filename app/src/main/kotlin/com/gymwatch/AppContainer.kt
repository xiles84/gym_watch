package com.gymwatch

import android.content.Context
import android.content.Intent
import com.gymwatch.adapters.driven.persistence.PersistenceAdapters
import com.gymwatch.adapters.driven.platform.AndroidClock
import com.gymwatch.adapters.driven.platform.AndroidHaptics
import com.gymwatch.adapters.driven.platform.AndroidWakeUp
import com.gymwatch.adapters.driven.platform.GymNotifications
import com.gymwatch.adapters.driven.platform.MediaAppLauncher
import com.gymwatch.adapters.driven.platform.OngoingActivityAdapter
import com.gymwatch.adapters.driven.platform.SamsungHealthIcons
import com.gymwatch.adapters.driven.platform.SamsungHealthLauncher
import com.gymwatch.adapters.driven.wearsync.DataLayerAudiobookPlayer
import com.gymwatch.adapters.driven.wearsync.DataLayerRecentAudiobooks
import com.gymwatch.core.application.AudiobooksUseCase
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.MediaShortcutsUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.ScreenLayoutUseCase
import com.gymwatch.core.application.SkinUseCase
import com.gymwatch.core.application.WorkoutSetupUseCase
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

    /** Samsung Health owns the workout record; we open it on the exercise (LESSONS.md #2, #28). */
    private val healthApp = SamsungHealthLauncher(context)

    /** Samsung Health's own icons, read from it at runtime and never bundled. */
    val workoutIcons = SamsungHealthIcons(context)

    val chronometer = ChronometerUseCase(AndroidClock, haptics)

    val restTimer = RestTimerUseCase(
        clock = AndroidClock,
        setup = persistence.workoutSetup,
        haptics = haptics,
        wakeUp = AndroidWakeUp(context),
        scope = scope,
    )

    val counter = CounterUseCase(persistence.counters, haptics, scope)

    val workouts = WorkoutSetupUseCase(
        repository = persistence.workoutSetup,
        healthApp = healthApp,
        haptics = haptics,
        scope = scope,
    )

    val screenLayout = ScreenLayoutUseCase(
        repository = persistence.screenLayout,
        haptics = haptics,
        scope = scope,
    )

    val skins = SkinUseCase(
        repository = persistence.skins,
        haptics = haptics,
        scope = scope,
    )

    /**
     * The phone plays; the watch shows the list the phone app syncs and asks it
     * to start a book (docs/LESSONS.md #32).
     */
    val audiobooks = AudiobooksUseCase(
        repository = DataLayerRecentAudiobooks(context),
        player = DataLayerAudiobookPlayer(context),
        haptics = haptics,
        scope = scope,
    )

    /** Spotify and the phone's media controls; also reads their launcher icons. */
    val mediaApps = MediaAppLauncher(context)

    val mediaShortcuts = MediaShortcutsUseCase(mediaApps, haptics)
}
