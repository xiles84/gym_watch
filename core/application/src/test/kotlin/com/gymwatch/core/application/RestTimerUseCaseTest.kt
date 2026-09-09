package com.gymwatch.core.application

import com.gymwatch.core.application.fake.InMemoryRestTimerSettings
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.application.fake.SchedulerClock
import com.gymwatch.core.domain.model.Haptic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerUseCaseTest {

    @Test
    fun `fires exactly once when the countdown reaches zero`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = RestTimerUseCase(
            clock = SchedulerClock(testScheduler),
            settings = InMemoryRestTimerSettings(60.seconds),
            haptics = haptics,
            scope = backgroundScope,
        )
        runCurrent()

        useCase.start()
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
            SchedulerClock(testScheduler), InMemoryRestTimerSettings(30.seconds),
            haptics, backgroundScope,
        )
        runCurrent()

        useCase.start()
        advanceTimeBy(20.minutes); runCurrent()

        assertEquals(1, haptics.played.count { it == Haptic.REST_OVER })
    }

    @Test
    fun `cancelling before the deadline never fires`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), InMemoryRestTimerSettings(60.seconds),
            haptics, backgroundScope,
        )
        runCurrent()

        useCase.start()
        advanceTimeBy(30.seconds); runCurrent()
        useCase.cancel()
        advanceTimeBy(5.minutes); runCurrent()

        assertTrue(haptics.played.none { it == Haptic.REST_OVER })
    }

    @Test
    fun `remaining counts down under virtual time`() = runTest {
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), InMemoryRestTimerSettings(90.seconds),
            RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        useCase.start()
        advanceTimeBy(30.seconds); runCurrent()
        assertEquals(60.seconds, useCase.remainingNow())
    }

    @Test
    fun `adjusting the length persists it for next time`() = runTest {
        val settings = InMemoryRestTimerSettings(60.seconds)
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), settings, RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        useCase.adjustBy(15.seconds)
        runCurrent()

        assertEquals(75.seconds, useCase.state.value.duration)
        assertEquals(75.seconds, settings.saved)
    }

    @Test
    fun `a saved length is adopted while idle`() = runTest {
        val settings = InMemoryRestTimerSettings(45.seconds)
        val useCase = RestTimerUseCase(
            SchedulerClock(testScheduler), settings, RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        assertEquals(45.seconds, useCase.state.value.duration)
        assertEquals(Duration.ZERO, useCase.state.value.remainingAt(0.seconds) - 45.seconds)
    }
}
