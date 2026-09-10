package com.gymwatch.core.application

import com.gymwatch.core.application.fake.InMemoryWorkoutSetupRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.application.fake.SchedulerClock
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.ResetOutcome
import com.gymwatch.core.domain.model.RestPresets
import com.gymwatch.core.domain.model.WorkoutSetup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
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

    private fun setupOf(vararg presets: Int) = InMemoryWorkoutSetupRepository(
        WorkoutSetup.DEFAULT.copy(restPresets = RestPresets.of(presets.map { it.seconds })),
    )

    private fun TestScope.restTimer(
        haptics: RecordingHaptics = RecordingHaptics(),
        setup: InMemoryWorkoutSetupRepository = setupOf(30, 60, 120),
    ) = RestTimerUseCase(SchedulerClock(testScheduler), setup, haptics, backgroundScope)

    private val RecordingHaptics.buzzes get() = played.count { it == Haptic.REST_OVER }

    @Test
    fun `the alarm starts at zero, not before`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(59.seconds); runCurrent()
        assertEquals(0, haptics.buzzes, "must not fire early")
        assertFalse(useCase.alarming.value)
        assertFalse(useCase.isAlarmingNow())
        assertTrue(useCase.needsResetConfirmationNow())

        advanceTimeBy(2.seconds); runCurrent()
        assertEquals(1, haptics.buzzes)
        assertTrue(useCase.alarming.value)
        assertTrue(useCase.isAlarmingNow())
        assertFalse(useCase.needsResetConfirmationNow())
        assertTrue(useCase.state.value.isRunning, "it stays on the alarm rather than going idle")
    }

    @Test
    fun `the alarm repeats until reset, and reset needs no confirmation`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(31.seconds); runCurrent()
        advanceTimeBy(RestTimerUseCase.ALARM_REPEAT); runCurrent()
        advanceTimeBy(RestTimerUseCase.ALARM_REPEAT); runCurrent()
        assertEquals(3, haptics.buzzes)

        assertEquals(ResetOutcome.DONE, useCase.requestReset())
        advanceTimeBy(5.minutes); runCurrent()

        assertEquals(3, haptics.buzzes, "reset silences it")
        assertFalse(useCase.alarming.value)
        assertFalse(useCase.state.value.isRunning)
    }

    @Test
    fun `resetting mid-countdown asks first and the countdown carries on`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(10.seconds); runCurrent()

        assertEquals(ResetOutcome.NEEDS_CONFIRMATION, useCase.requestReset())
        assertTrue(useCase.state.value.isRunning)

        advanceTimeBy(21.seconds); runCurrent()
        assertEquals(1, haptics.buzzes, "an unconfirmed reset does not stop the alarm")
    }

    @Test
    fun `a confirmed reset before zero never rings`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(30.seconds); runCurrent()
        useCase.confirmReset()
        advanceTimeBy(5.minutes); runCurrent()

        assertEquals(0, haptics.buzzes)
        assertFalse(useCase.alarming.value)
    }

    @Test
    fun `resetting an idle timer is done at once`() = runTest {
        val useCase = restTimer()
        runCurrent()

        assertEquals(ResetOutcome.DONE, useCase.requestReset())
    }

    @Test
    fun `each preset starts its own length`() = runTest {
        val useCase = restTimer(setup = setupOf(30, 90, 120))
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(30.seconds); runCurrent()

        assertEquals(60.seconds, useCase.remainingNow())
    }

    @Test
    fun `presets follow the stored setup`() = runTest {
        val repository = InMemoryWorkoutSetupRepository()
        val useCase = restTimer(setup = repository)
        runCurrent()

        assertEquals(WorkoutSetup.DEFAULT.restPresets, useCase.presets.value)

        val changed = WorkoutSetup.DEFAULT.withRestPresetAt(0, 45.seconds)
        repository.save(changed)
        runCurrent()

        assertEquals(changed.restPresets, useCase.presets.value)
    }

    @Test
    fun `starting reads the stored preset, not the seeded default`() = runTest {
        // The seeded StateFlow default is RestPresets.DEFAULT (60/90/120). A tap
        // landing before the first stored emission must still use the stored
        // 45s value. See docs/LESSONS.md #10.
        val useCase = restTimer(setup = setupOf(45, 90, 120))

        useCase.start(index = 0)
        runCurrent()

        assertEquals(45.seconds, useCase.state.value.duration)
    }
}
