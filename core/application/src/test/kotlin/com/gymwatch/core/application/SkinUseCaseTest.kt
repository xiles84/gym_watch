package com.gymwatch.core.application

import com.gymwatch.core.application.fake.InMemorySkinRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.Skin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SkinUseCaseTest {

    private class Fixture(initial: Skin = Skin.DEFAULT) {
        val scope = TestScope()
        val repository = InMemorySkinRepository(initial)
        val haptics = RecordingHaptics()
        val useCase = SkinUseCase(repository, haptics, scope)
    }

    @Test
    fun `selecting a skin persists it and confirms with a tick`() = runTest {
        val f = Fixture()
        f.useCase.select(Skin.EMBER)
        f.scope.runCurrent()

        assertEquals(Skin.EMBER, f.repository.saved)
        assertEquals(listOf(Haptic.TICK), f.haptics.played)
    }

    @Test
    fun `selecting the skin already in use writes nothing`() = runTest {
        val f = Fixture(Skin.ULTRAVIOLET)
        f.useCase.select(Skin.ULTRAVIOLET)
        f.scope.runCurrent()

        assertEquals(null, f.repository.saved)
        assertTrue(f.haptics.played.isEmpty(), "no feedback for a no-op")
    }

    @Test
    fun `a tap before the first emission is judged against the stored skin`() = runTest {
        // docs/LESSONS.md #10: `state` is seeded with DEFAULT until the
        // repository emits. Comparing against the seed would treat a tap on the
        // stored skin as a change and buzz for nothing.
        val f = Fixture(Skin.EMBER)
        assertEquals(Skin.DEFAULT, f.useCase.state.value, "the seed is still in place")

        f.useCase.select(Skin.EMBER)
        f.scope.runCurrent()

        assertEquals(null, f.repository.saved)
        assertTrue(f.haptics.played.isEmpty())
    }
}
