package com.gymwatch.core.application.fake

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Favourites
import com.gymwatch.core.domain.model.SessionOwnership
import com.gymwatch.core.domain.model.WorkoutSnapshot
import com.gymwatch.core.domain.model.WorkoutState
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.CounterRepositoryPort
import com.gymwatch.core.domain.port.FavouritesRepositoryPort
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.RestTimerSettingsPort
import com.gymwatch.core.domain.port.WorkoutSessionPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** A clock we move by hand. */
class FakeClock(var now: Duration = Duration.ZERO) : ClockPort {
    override fun elapsed(): Duration = now
    fun advance(by: Duration) { now += by }
}

/**
 * A clock tied to the coroutine test scheduler, so `delay()` in production code
 * and the clock the code reads move together under virtual time.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SchedulerClock(private val scheduler: TestCoroutineScheduler) : ClockPort {
    override fun elapsed(): Duration = scheduler.currentTime.milliseconds
}

class RecordingHaptics : HapticsPort {
    val played = mutableListOf<Haptic>()
    override fun play(haptic: Haptic) { played += haptic }
}

class InMemoryCounterRepository(initial: Counter = Counter()) : CounterRepositoryPort {
    private val flow = MutableStateFlow(initial)
    override val counter: StateFlow<Counter> = flow.asStateFlow()
    override suspend fun save(counter: Counter) { flow.value = counter }
}

class InMemoryFavouritesRepository(
    initial: Favourites = Favourites.DEFAULT,
) : FavouritesRepositoryPort {
    private val flow = MutableStateFlow(initial)
    override val favourites: StateFlow<Favourites> = flow.asStateFlow()
    override suspend fun save(favourites: Favourites) { flow.value = favourites }
}

class InMemoryRestTimerSettings(initial: Duration) : RestTimerSettingsPort {
    private val flow = MutableStateFlow(initial)
    override val duration: StateFlow<Duration> = flow.asStateFlow()
    var saved: Duration? = null
        private set
    override suspend fun save(duration: Duration) {
        saved = duration
        flow.value = duration
    }
}

class FakeWorkoutSession(
    private val supported: Set<ExerciseKind> = ExerciseKind.entries.toSet(),
    var ownership: SessionOwnership = SessionOwnership.NONE,
) : WorkoutSessionPort {
    private val flow = MutableStateFlow<WorkoutSnapshot?>(null)
    override val snapshots: StateFlow<WorkoutSnapshot?> = flow.asStateFlow()

    val startedKinds = mutableListOf<ExerciseKind>()
    var ended = false
        private set

    override suspend fun supportedKinds(): Set<ExerciseKind> = supported
    override suspend fun ownership(): SessionOwnership = ownership

    override suspend fun start(kind: ExerciseKind) {
        startedKinds += kind
        ownership = SessionOwnership.OURS
        flow.value = WorkoutSnapshot(kind, WorkoutState.ACTIVE, Duration.ZERO)
    }

    override suspend fun pause() {
        flow.value = flow.value?.copy(state = WorkoutState.PAUSED)
    }

    override suspend fun resume() {
        flow.value = flow.value?.copy(state = WorkoutState.ACTIVE)
    }

    override suspend fun end() {
        ended = true
        ownership = SessionOwnership.NONE
        flow.value = null
    }
}
