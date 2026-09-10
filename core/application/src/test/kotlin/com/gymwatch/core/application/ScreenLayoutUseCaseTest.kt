package com.gymwatch.core.application

import com.gymwatch.core.application.fake.InMemoryScreenLayoutRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.AppScreen
import com.gymwatch.core.domain.model.ScreenLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenLayoutUseCaseTest {

    private class Fixture(initial: ScreenLayout = ScreenLayout.DEFAULT) {
        val scope = TestScope()
        val repository = InMemoryScreenLayoutRepository(initial)
        val haptics = RecordingHaptics()
        val useCase = ScreenLayoutUseCase(repository, haptics, scope)
    }

    @Test
    fun `hiding a screen persists it and shortens the pager`() = runTest {
        val f = Fixture()
        f.useCase.toggle(AppScreen.COUNTER)
        f.scope.runCurrent()

        val saved = requireNotNull(f.repository.saved)
        assertEquals(setOf(AppScreen.COUNTER), saved.hidden)
        assertEquals(3, saved.visible.size)
    }

    @Test
    fun `hiding the last visible screen is refused`() = runTest {
        val allButOne = ScreenLayout(
            order = AppScreen.entries.toList(),
            hidden = AppScreen.entries.toSet() - AppScreen.CHRONOMETER,
        )
        val f = Fixture(allButOne)

        f.useCase.toggle(AppScreen.CHRONOMETER)
        f.scope.runCurrent()

        assertEquals(null, f.repository.saved, "no write for a refused change")
        assertTrue(f.haptics.played.isEmpty(), "no feedback for a no-op")
    }

    @Test
    fun `a hidden screen comes back where it was, not at the end`() = runTest {
        val f = Fixture()
        f.useCase.toggle(AppScreen.REST_TIMER)
        f.scope.runCurrent()
        f.useCase.toggle(AppScreen.REST_TIMER)
        f.scope.runCurrent()

        assertEquals(ScreenLayout.DEFAULT.order, requireNotNull(f.repository.saved).order)
    }

    @Test
    fun `moving a screen reorders the pager`() = runTest {
        val f = Fixture()
        f.useCase.move(AppScreen.WORKOUTS, -1)
        f.scope.runCurrent()

        assertEquals(
            listOf(
                AppScreen.CHRONOMETER,
                AppScreen.REST_TIMER,
                AppScreen.WORKOUTS,
                AppScreen.COUNTER,
            ),
            requireNotNull(f.repository.saved).order,
        )
    }

    @Test
    fun `moving off the end does nothing`() = runTest {
        val f = Fixture()
        f.useCase.move(AppScreen.CHRONOMETER, -1)
        f.scope.runCurrent()

        assertEquals(null, f.repository.saved)
    }
}
