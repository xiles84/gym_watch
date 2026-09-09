package com.gymwatch.adapters.driven.health

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseEndReason
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseUpdate
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.SessionOwnership
import com.gymwatch.core.domain.model.WorkoutEndReason
import com.gymwatch.core.domain.model.WorkoutSnapshot
import com.gymwatch.core.domain.model.WorkoutState
import com.gymwatch.core.domain.port.WorkoutSessionPort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import java.time.Duration as JavaDuration

/**
 * WorkoutSessionPort over Health Services ExerciseClient.
 *
 * Everything platform-specific about workouts is confined here: the
 * one-exercise-at-a-time rule, the checkpoint arithmetic, and the fact that a
 * session can be ended by someone other than the user.
 */
class HealthServicesWorkoutSession(context: Context) : WorkoutSessionPort {

    private val exerciseClient = HealthServices.getClient(context).exerciseClient

    private val _snapshots = MutableStateFlow<WorkoutSnapshot?>(null)
    override val snapshots: Flow<WorkoutSnapshot?> = _snapshots.asStateFlow()

    /** The kind we asked for; an update does not always echo it back. */
    private var requestedKind: ExerciseKind? = null

    private val callback = object : ExerciseUpdateCallback {

        override fun onRegistered() = Unit

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.w(TAG, "Exercise update registration failed", throwable)
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val kind = update.exerciseConfig?.exerciseType?.toExerciseKind()
                ?: requestedKind
                ?: return

            val state = update.exerciseStateInfo.state
            if (state.isEnded) {
                Log.i(TAG, "Exercise ended: " + state.name + " reason=" + update.exerciseStateInfo.endReason)
                requestedKind = null
                _snapshots.value = null
                return
            }

            _snapshots.value = WorkoutSnapshot(
                kind = kind,
                state = when {
                    state.isPaused -> WorkoutState.PAUSED
                    state == ExerciseState.PREPARING -> WorkoutState.PREPARING
                    else -> WorkoutState.ACTIVE
                },
                activeDuration = update.activeDurationNow(),
                heartRateBpm = update.latestMetrics
                    .getData(DataType.HEART_RATE_BPM)
                    .lastOrNull()
                    ?.value
                    ?.toInt(),
                calories = update.latestMetrics
                    .getData(DataType.CALORIES_TOTAL)
                    ?.total
                    ?.toInt(),
            )
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
    }

    override suspend fun supportedKinds(): Set<ExerciseKind> = runCatching {
        val supported = exerciseClient.getCapabilitiesAsync().await().supportedExerciseTypes
        ExerciseKind.entries.filter { it.toExerciseType() in supported }.toSet()
    }.getOrElse {
        // Health Services missing or unavailable: report nothing rather than
        // offering buttons that will fail. The UI already handles an empty set.
        Log.w(TAG, "Could not read exercise capabilities", it)
        emptySet()
    }

    // ExerciseTrackedStatus.Companion carries @RestrictTo(LIBRARY) in
    // health-services-client 1.1.0-rc02, even though the constants are public
    // static ints on the interface and Google's own docs use them by name.
    // A library packaging bug, not a real API boundary. See docs/LESSONS.md #21.
    @SuppressLint("RestrictedApi")
    override suspend fun ownership(): SessionOwnership = runCatching {
        when (exerciseClient.getCurrentExerciseInfoAsync().await().exerciseTrackedStatus) {
            ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS -> SessionOwnership.OURS
            ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS -> SessionOwnership.OTHER_APP
            else -> SessionOwnership.NONE
        }
    }.getOrElse {
        Log.w(TAG, "Could not read current exercise info", it)
        SessionOwnership.NONE
    }

    override suspend fun start(kind: ExerciseKind) {
        requestedKind = kind
        exerciseClient.setUpdateCallback(callback)

        val capabilities = exerciseClient.getCapabilitiesAsync().await()
            .getExerciseTypeCapabilities(kind.toExerciseType())

        // Ask only for what this watch can actually deliver for this exercise:
        // requesting an unsupported DataType fails the whole start call.
        val dataTypes = setOf(DataType.HEART_RATE_BPM, DataType.CALORIES_TOTAL)
            .filterTo(mutableSetOf()) { it in capabilities.supportedDataTypes }

        exerciseClient.startExerciseAsync(
            ExerciseConfig(
                exerciseType = kind.toExerciseType(),
                dataTypes = dataTypes,
                isAutoPauseAndResumeEnabled = false,
                isGpsEnabled = false,
            ),
        ).await()
    }

    override suspend fun pause() {
        runCatching { exerciseClient.pauseExerciseAsync().await() }
            .onFailure { Log.w(TAG, "Pause failed", it) }
    }

    override suspend fun resume() {
        runCatching { exerciseClient.resumeExerciseAsync().await() }
            .onFailure { Log.w(TAG, "Resume failed", it) }
    }

    override suspend fun end() {
        runCatching { exerciseClient.endExerciseAsync().await() }
            .onFailure { Log.w(TAG, "End failed", it) }
        requestedKind = null
        _snapshots.value = null
    }

    private companion object {
        const val TAG = "GymWatch"
    }
}

/**
 * Elapsed active time, per the documented formula:
 * (now - checkpoint.time) + checkpoint.activeDuration.
 *
 * A local tick counter drifts and double-counts across pause/resume, which is
 * exactly the bug this avoids (docs/LESSONS.md #6).
 */
private fun ExerciseUpdate.activeDurationNow(): Duration {
    val checkpoint = activeDurationCheckpoint ?: return Duration.ZERO
    val since = JavaDuration.between(checkpoint.time, Instant.now())
    val safe = if (since.isNegative) JavaDuration.ZERO else since
    return checkpoint.activeDuration.plus(safe).toKotlinDuration()
}

@SuppressLint("RestrictedApi") // same @RestrictTo(LIBRARY) companion issue
internal fun Int.toWorkoutEndReason(): WorkoutEndReason = when (this) {
    ExerciseEndReason.USER_END -> WorkoutEndReason.USER_ENDED
    ExerciseEndReason.AUTO_END_SUPERSEDED -> WorkoutEndReason.TAKEN_OVER
    ExerciseEndReason.AUTO_END_PERMISSION_LOST -> WorkoutEndReason.AUTO_ENDED_PERMISSION_LOST
    else -> WorkoutEndReason.UNKNOWN
}
