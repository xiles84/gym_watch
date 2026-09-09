package com.gymwatch.core.application

import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Favourites
import com.gymwatch.core.domain.model.SessionOwnership
import com.gymwatch.core.domain.model.WorkoutSnapshot
import com.gymwatch.core.domain.port.FavouritesRepositoryPort
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.WorkoutSessionPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Result of asking to start a workout. The conflict case is explicit on purpose. */
sealed interface StartOutcome {
    data class Started(val kind: ExerciseKind) : StartOutcome

    /** Another app (in practice: Samsung Health) owns the single exercise slot. */
    data class NeedsConfirmation(val kind: ExerciseKind) : StartOutcome

    /** We are already running this; the UI should just show the session screen. */
    data object AlreadyRunning : StartOutcome

    data class Unsupported(val kind: ExerciseKind) : StartOutcome
}

class WorkoutUseCase(
    private val session: WorkoutSessionPort,
    private val favouritesRepository: FavouritesRepositoryPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    val snapshot: StateFlow<WorkoutSnapshot?> =
        session.snapshots.stateIn(scope, SharingStarted.Eagerly, null)

    val favourites: StateFlow<Favourites> =
        favouritesRepository.favourites.stateIn(scope, SharingStarted.Eagerly, Favourites.DEFAULT)

    suspend fun supportedKinds(): Set<ExerciseKind> = session.supportedKinds()

    /**
     * Health Services permits one exercise device-wide. We never take that slot
     * without asking, so a foreign session returns [StartOutcome.NeedsConfirmation]
     * instead of quietly killing someone else's workout.
     */
    suspend fun requestStart(kind: ExerciseKind): StartOutcome {
        if (kind !in session.supportedKinds()) return StartOutcome.Unsupported(kind)
        return when (session.ownership()) {
            SessionOwnership.OURS -> StartOutcome.AlreadyRunning
            SessionOwnership.OTHER_APP -> StartOutcome.NeedsConfirmation(kind)
            SessionOwnership.NONE -> {
                forceStart(kind)
                StartOutcome.Started(kind)
            }
        }
    }

    /** Only call after the user has confirmed a [StartOutcome.NeedsConfirmation]. */
    suspend fun forceStart(kind: ExerciseKind) {
        session.start(kind)
        haptics.play(Haptic.CONFIRM)
    }

    suspend fun pause() = session.pause()

    suspend fun resume() = session.resume()

    suspend fun end() {
        session.end()
        haptics.play(Haptic.CONFIRM)
    }

    fun toggleFavourite(kind: ExerciseKind) {
        // Reads the repository rather than the seeded StateFlow, for the same
        // reason as CounterUseCase.mutate.
        scope.launch {
            val current = favouritesRepository.favourites.first()
            val next = current.toggle(kind)
            if (next == current) return@launch
            haptics.play(Haptic.TICK)
            favouritesRepository.save(next)
        }
    }
}
