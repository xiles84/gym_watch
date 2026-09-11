package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeWakeUp
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
import kotlin.test.assertNull
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
        clock: SchedulerClock = SchedulerClock(testScheduler),
        wakeUp: FakeWakeUp = FakeWakeUp(),
    ) = RestTimerUseCase(clock, setup, haptics, wakeUp, backgroundScope)

    private val RecordingHaptics.buzzes get() = played.count { it == Haptic.REST_OVER }

    @Test
    fun `a countdown books a wake-up for its zero`() = runTest {
        val clock = SchedulerClock(testScheduler)
        val wakeUp = FakeWakeUp()
        val useCase = restTimer(clock = clock, wakeUp = wakeUp)
        runCurrent()
        advanceTimeBy(7.seconds)

        useCase.start(index = 0)
        runCurrent()

        assertEquals(clock.elapsed() + 30.seconds, wakeUp.pendingAt)
    }

    @Test
    fun `the alarm rings on time when the watch slept through zero`() = runTest {
        // The bug on the watch: the screen went off mid-rest, the CPU suspended,
        // and the watchdog's delay stopped counting. The clock says zero; the
        // delay still thinks there are 20 s to go.
        val haptics = RecordingHaptics()
        val clock = SchedulerClock(testScheduler)
        val wakeUp = FakeWakeUp()
        val useCase = restTimer(haptics, clock = clock, wakeUp = wakeUp)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(10.seconds); runCurrent()
        clock.deepSleep(20.seconds)

        wakeUp.fire()
        runCurrent()

        assertEquals(1, haptics.buzzes, "rings when woken, not when the stalled delay ends")
        assertTrue(useCase.alarming.value)
        assertTrue(wakeUp.awake, "held awake so the repeats are not stalled too")

        advanceTimeBy(RestTimerUseCase.ALARM_REPEAT); runCurrent()
        assertEquals(2, haptics.buzzes)
        advanceTimeBy(20.seconds); runCurrent()
        // Every 3 s from the wake at 10 s: 13, then 16 through 31.
        assertEquals(8, haptics.buzzes, "the stale delay ending does not add a second ring")
    }

    @Test
    fun `a wake-up after the watchdog already rang changes nothing`() = runTest {
        val haptics = RecordingHaptics()
        val wakeUp = FakeWakeUp()
        val useCase = restTimer(haptics, wakeUp = wakeUp)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(30.seconds); runCurrent()
        assertEquals(1, haptics.buzzes)

        wakeUp.fire()
        runCurrent()
        assertEquals(1, haptics.buzzes)
    }

    @Test
    fun `stopping releases the wake-up and the wake lock`() = runTest {
        val wakeUp = FakeWakeUp()
        val useCase = restTimer(wakeUp = wakeUp)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(31.seconds); runCurrent()
        assertTrue(wakeUp.awake)

        useCase.requestStop()
        assertFalse(wakeUp.awake)
        assertNull(wakeUp.pendingAt)
    }

    @Test
    fun `a restart books the new zero and holds nothing awake`() = runTest {
        val haptics = RecordingHaptics()
        val clock = SchedulerClock(testScheduler)
        val wakeUp = FakeWakeUp()
        val useCase = restTimer(haptics, clock = clock, wakeUp = wakeUp)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(31.seconds); runCurrent()

        useCase.requestRestart()
        runCurrent()

        assertFalse(wakeUp.awake)
        assertEquals(clock.elapsed() + 30.seconds, wakeUp.pendingAt)
    }

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
    fun `the alarm repeats until stopped, and stopping needs no confirmation`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(31.seconds); runCurrent()
        advanceTimeBy(RestTimerUseCase.ALARM_REPEAT); runCurrent()
        advanceTimeBy(RestTimerUseCase.ALARM_REPEAT); runCurrent()
        assertEquals(3, haptics.buzzes)

        assertEquals(ResetOutcome.DONE, useCase.requestStop())
        advanceTimeBy(5.minutes); runCurrent()

        assertEquals(3, haptics.buzzes, "stopping silences it")
        assertFalse(useCase.alarming.value)
        assertFalse(useCase.state.value.isRunning)
    }

    @Test
    fun `stopping mid-countdown asks first and the countdown carries on`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(10.seconds); runCurrent()

        assertEquals(ResetOutcome.NEEDS_CONFIRMATION, useCase.requestStop())
        assertTrue(useCase.state.value.isRunning)

        advanceTimeBy(21.seconds); runCurrent()
        assertEquals(1, haptics.buzzes, "an unconfirmed stop does not stop the alarm")
    }

    @Test
    fun `a confirmed stop before zero never rings`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(30.seconds); runCurrent()
        useCase.confirmStop()
        advanceTimeBy(5.minutes); runCurrent()

        assertEquals(0, haptics.buzzes)
        assertFalse(useCase.alarming.value)
    }

    @Test
    fun `stopping an idle timer is done at once`() = runTest {
        val useCase = restTimer()
        runCurrent()

        assertEquals(ResetOutcome.DONE, useCase.requestStop())
    }

    @Test
    fun `restarting at the alarm silences it and runs the same length again`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 0)
        runCurrent()
        advanceTimeBy(31.seconds); runCurrent()
        assertEquals(1, haptics.buzzes)

        assertEquals(ResetOutcome.DONE, useCase.requestRestart(), "at zero it does not ask")
        runCurrent()
        assertFalse(useCase.alarming.value)
        assertFalse(useCase.isAlarmingNow())
        assertEquals(30.seconds, useCase.remainingNow())

        advanceTimeBy(29.seconds); runCurrent()
        assertEquals(1, haptics.buzzes, "quiet until the new countdown ends")

        advanceTimeBy(2.seconds); runCurrent()
        assertEquals(2, haptics.buzzes)
        assertTrue(useCase.alarming.value)
    }

    @Test
    fun `restarting mid-countdown asks first, and once confirmed starts from full`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = restTimer(haptics)
        runCurrent()

        useCase.start(index = 1)
        runCurrent()
        advanceTimeBy(40.seconds); runCurrent()

        assertEquals(ResetOutcome.NEEDS_CONFIRMATION, useCase.requestRestart())
        assertEquals(20.seconds, useCase.remainingNow(), "an unconfirmed restart changes nothing")

        useCase.confirmRestart()
        runCurrent()
        assertEquals(60.seconds, useCase.remainingNow())

        advanceTimeBy(59.seconds); runCurrent()
        assertEquals(0, haptics.buzzes, "the old countdown's zero no longer rings")
    }

    @Test
    fun `restarting an idle or stopped timer leaves it idle`() = runTest {
        val useCase = restTimer()
        runCurrent()

        assertEquals(ResetOutcome.DONE, useCase.requestRestart())
        assertFalse(useCase.state.value.isRunning)

        useCase.start(index = 0)
        runCurrent()
        useCase.confirmStop()
        useCase.confirmRestart()
        runCurrent()

        assertFalse(useCase.state.value.isRunning)
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
