package com.gymwatch.core.application

import com.gymwatch.core.application.fake.InMemoryCounterRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.Counter
import com.gymwatch.core.domain.model.Haptic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CounterUseCaseTest {

    private fun scope(initial: Counter = Counter()) =
        TestScope().let { s ->
            val repo = InMemoryCounterRepository(initial)
            val haptics = RecordingHaptics()
            Triple(CounterUseCase(repo, haptics, s), haptics, s)
        }

    @Test
    fun `increment persists through the repository`() = runTest {
        val (useCase, _, s) = scope()
        useCase.increment()
        s.runCurrent()
        assertEquals(1, useCase.state.value.value)
    }

    @Test
    fun `decrementing at zero does not write or buzz`() = runTest {
        val (useCase, haptics, s) = scope(Counter(value = 0))
        useCase.decrement()
        s.runCurrent()
        assertEquals(0, useCase.state.value.value)
        assertTrue(haptics.played.isEmpty(), "no feedback for a no-op")
    }

    @Test
    fun `rotary steps accumulate`() = runTest {
        val (useCase, _, s) = scope()
        repeat(5) { useCase.stepBy(1); s.runCurrent() }
        useCase.stepBy(-2); s.runCurrent()
        assertEquals(3, useCase.state.value.value)
    }

    @Test
    fun `reset buzzes with confirmation and zeroes the value`() = runTest {
        val (useCase, haptics, s) = scope(Counter(value = 12))
        useCase.reset()
        s.runCurrent()
        assertEquals(0, useCase.state.value.value)
        assertEquals(listOf(Haptic.CONFIRM), haptics.played)
    }

    @Test
    fun `label survives a reset`() = runTest {
        val (useCase, _, s) = scope(Counter(value = 8, label = "BENCH"))
        useCase.reset()
        s.runCurrent()
        assertEquals("BENCH", useCase.state.value.label)
    }
}
