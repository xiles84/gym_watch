package com.gymwatch

import android.content.Context
import android.content.Intent
import com.gymwatch.adapters.driven.persistence.PersistenceAdapters
import com.gymwatch.adapters.driven.platform.AndroidClock
import com.gymwatch.adapters.driven.platform.AndroidHaptics
import com.gymwatch.adapters.driven.platform.AndroidPermissions
import com.gymwatch.adapters.driven.platform.GymNotifications
import com.gymwatch.adapters.driven.platform.OngoingActivityAdapter
import com.gymwatch.core.application.ChronometerUseCase
import com.gymwatch.core.application.CounterUseCase
import com.gymwatch.core.application.RestTimerUseCase
import com.gymwatch.core.application.WorkoutUseCase
import com.gymwatch.core.domain.port.WorkoutSessionPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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

    val permissions = AndroidPermissions(context)

    val ongoingActivity = OngoingActivityAdapter(
        context = context,
        notifications = notifications,
        launchIntent = {
            Intent(context, MainActivity::class.java)
        },
    )

    private val persistence = PersistenceAdapters(context)

    val chronometer = ChronometerUseCase(AndroidClock, haptics)

    val restTimer = RestTimerUseCase(
        clock = AndroidClock,
        settings = persistence.restTimerSettings,
        haptics = haptics,
        scope = scope,
    )

    val counter = CounterUseCase(persistence.counters, haptics, scope)

    val workouts = WorkoutUseCase(
        session = NotYetImplementedWorkoutSession,
        favouritesRepository = persistence.favourites,
        haptics = haptics,
        scope = scope,
    )
}

/**
 * Placeholder until phase 4 swaps in the Health Services adapter.
 *
 * It reports no supported exercise kinds, so `WorkoutUseCase.requestStart`
 * returns `Unsupported` and the UI says so honestly rather than appearing to
 * start a workout that is not being recorded. Replacing this is a one-line
 * change in [AppContainer] — which is the point of the port.
 */
private object NotYetImplementedWorkoutSession : WorkoutSessionPort {
    override val snapshots: Flow<Nothing?> = flowOf(null)
    override suspend fun supportedKinds() = emptySet<Nothing>()
    override suspend fun ownership() = com.gymwatch.core.domain.model.SessionOwnership.NONE
    override suspend fun start(kind: com.gymwatch.core.domain.model.ExerciseKind) = Unit
    override suspend fun pause() = Unit
    override suspend fun resume() = Unit
    override suspend fun end() = Unit
}
