package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeClock
import com.gymwatch.core.application.fake.RecordingHaptics
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
    fun `reset returns to idle`() {
        useCase.toggle()
        clock.advance(1.minutes)
        useCase.reset()
        assertTrue(useCase.state.value.isReset)
        assertEquals(kotlin.time.Duration.ZERO, useCase.elapsedNow())
    }
}
