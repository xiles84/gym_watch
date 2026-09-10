package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeClock
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.ResetOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChronometerUseCaseTest {

    private val clock = FakeClock()
    private val haptics = RecordingHaptics()
    private val useCase = ChronometerUseCase(clock, haptics)

    @Test
    fun `toggle starts then pauses`() {
        useCase.toggle()
        assertTrue(useCase.state.value.isRunning)

        clock.advance(30.seconds)
        useCase.toggle()

        assertFalse(useCase.state.value.isRunning)
        assertEquals(30.seconds, useCase.elapsedNow())
    }

    @Test
    fun `elapsed keeps up with the clock while nothing runs in between`() {
        useCase.toggle()
        clock.advance(42.minutes)
        assertEquals(42.minutes, useCase.elapsedNow())
    }

    @Test
    fun `laps split the elapsed time`() {
        useCase.toggle()
        clock.advance(20.seconds); useCase.lap()
        clock.advance(35.seconds)

        assertEquals(listOf(20.seconds), useCase.state.value.laps)
        assertEquals(35.seconds, useCase.currentLapNow())
    }

    @Test
    fun `lap while idle changes nothing and stays silent`() {
        useCase.lap()
        assertTrue(useCase.state.value.laps.isEmpty())
        assertTrue(haptics.played.isEmpty())
    }

    @Test
    fun `resetting a running chronometer asks first and changes nothing`() {
        useCase.toggle()
        clock.advance(1.minutes)
        haptics.played.clear()

        assertEquals(ResetOutcome.NEEDS_CONFIRMATION, useCase.requestReset())
        assertTrue(useCase.state.value.isRunning)
        assertEquals(1.minutes, useCase.elapsedNow())
        assertTrue(haptics.played.isEmpty(), "no feedback until the user confirms")
    }

    @Test
    fun `resetting a paused chronometer happens at once`() {
        useCase.toggle()
        clock.advance(1.minutes)
        useCase.toggle()

        assertEquals(ResetOutcome.DONE, useCase.requestReset())
        assertTrue(useCase.state.value.isReset)
    }

    @Test
    fun `confirming a reset clears a running chronometer`() {
        useCase.toggle()
        clock.advance(1.minutes)

        useCase.confirmReset()

        assertTrue(useCase.state.value.isReset)
        assertEquals(kotlin.time.Duration.ZERO, useCase.elapsedNow())
    }
}
