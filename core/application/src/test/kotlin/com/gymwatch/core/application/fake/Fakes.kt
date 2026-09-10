package com.gymwatch.core.application.fake

import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.ScreenLayout
import com.gymwatch.core.domain.model.Skin
import com.gymwatch.core.domain.model.WorkoutSetup
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.CompanionHealthAppPort
import com.gymwatch.core.domain.port.CounterRepositoryPort
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.ScreenLayoutRepositoryPort
import com.gymwatch.core.domain.port.SkinRepositoryPort
import com.gymwatch.core.domain.port.WorkoutSetupRepositoryPort
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

class InMemoryWorkoutSetupRepository(
    initial: WorkoutSetup = WorkoutSetup.DEFAULT,
) : WorkoutSetupRepositoryPort {
    private val flow = MutableStateFlow(initial)
    override val setup: StateFlow<WorkoutSetup> = flow.asStateFlow()
    var saved: WorkoutSetup? = null
        private set
    override suspend fun save(setup: WorkoutSetup) {
        saved = setup
        flow.value = setup
    }
}

class InMemoryScreenLayoutRepository(
    initial: ScreenLayout = ScreenLayout.DEFAULT,
) : ScreenLayoutRepositoryPort {
    private val flow = MutableStateFlow(initial)
    override val layout: StateFlow<ScreenLayout> = flow.asStateFlow()
    var saved: ScreenLayout? = null
        private set
    override suspend fun save(layout: ScreenLayout) {
        saved = layout
        flow.value = layout
    }
}

/**
 * @param available whether the health app is installed at all.
 * @param startSucceeds whether the direct route into an exercise still works —
 *   false models an app update that removed it.
 */
class FakeHealthApp(
    private val available: Boolean = true,
    private val startSucceeds: Boolean = true,
) : CompanionHealthAppPort {
    var openCount = 0
        private set
    val started = mutableListOf<ExerciseKind>()

    override suspend fun isAvailable(): Boolean = available

    override suspend fun open(): Boolean {
        if (!available) return false
        openCount++
        return true
    }

    override suspend fun startWorkout(kind: ExerciseKind): Boolean {
        if (!available) return false
        started += kind
        return startSucceeds
    }
}

class InMemorySkinRepository(
    initial: Skin = Skin.DEFAULT,
) : SkinRepositoryPort {
    private val flow = MutableStateFlow(initial)
    override val skin: StateFlow<Skin> = flow.asStateFlow()
    var saved: Skin? = null
        private set
    override suspend fun save(skin: Skin) {
        saved = skin
        flow.value = skin
    }
}
