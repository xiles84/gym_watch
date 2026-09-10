package com.gymwatch.core.application

import com.gymwatch.core.application.fake.InMemoryProfilesRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.application.fake.SchedulerClock
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.Profiles
import com.gymwatch.core.domain.model.RestPresets
import com.gymwatch.core.domain.model.WorkoutProfile
import com.gymwatch.core.domain.model.ExerciseKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerUseCaseTest {

    private fun profilesOf(vararg presets: Int) = InMemoryProfilesRepository(
        Profiles(
            listOf(
                WorkoutProfile(
                    kind = ExerciseKind.WEIGHTS,
                    restPresets = RestPresets.of(presets.map { it.seconds }),
                ),
                Profiles.DEFAULT.entries[1],
                Profiles.DEFAULT.entries[2],
            ),
        ),
    )

    @Test
    fun `fires exactly once when the countdown reaches zero`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = RestTimerUseCase(
            clock = SchedulerClock(testScheduler),
            profiles = profilesOf(30, 60, 120),
            haptics = haptics,
            scope = backgroundScope,
        )
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(59.seconds); runCurrent()
        assertTrue(haptics.played.isEmpty(), "must not fire early")

        advanceTimeBy(2.seconds); runCurrent()
        assertEquals(listOf(Haptic.REST_OVER), haptics.played)
        assertFalse(useCase.state.value.isRunning, "timer returns to idle after firing")
    }

    @Test
    fun `a long doze past the deadline still fires only once`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), profilesOf(30, 60, 120), haptics, backgroundScope,
        )
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(20.minutes); runCurrent()

        assertEquals(1, haptics.played.count { it == Haptic.REST_OVER })
    }

    @Test
    fun `cancelling before the deadline never fires`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), profilesOf(30, 60, 120), haptics, backgroundScope,
        )
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(30.seconds); runCurrent()
        useCase.cancel()
        advanceTimeBy(5.minutes); runCurrent()

        assertTrue(haptics.played.none { it == Haptic.REST_OVER })
    }

    @Test
    fun `each preset starts its own length`() = runTest {
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), profilesOf(30, 90, 120),
            RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(30.seconds); runCurrent()

        assertEquals(60.seconds, useCase.remainingNow())
    }

    @Test
    fun `presets follow the active profile`() = runTest {
        val repository = InMemoryProfilesRepository()
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), repository, RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        assertEquals(Profiles.DEFAULT.entries[0].restPresets, useCase.presets.value)

        repository.save(Profiles.DEFAULT.select(1))
        runCurrent()

        assertEquals(Profiles.DEFAULT.entries[1].restPresets, useCase.presets.value)
    }

    @Test
    fun `starting reads the stored preset, not the seeded default`() = runTest {
        // The seeded StateFlow default is RestPresets.DEFAULT (60/90/120). A tap
        // landing before the first stored emission must still use the stored
        // 45s value. See docs/LESSONS.md #10.
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), profilesOf(45, 90, 120),
            RecordingHaptics(), backgroundScope,
        )

        useCase.start(index = 0)
        runCurrent()

        assertEquals(45.seconds, useCase.state.value.duration)
    }
}
