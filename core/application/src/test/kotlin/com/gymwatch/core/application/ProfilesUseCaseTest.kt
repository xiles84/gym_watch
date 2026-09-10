package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeHealthApp
import com.gymwatch.core.application.fake.InMemoryProfilesRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.CounterLabel
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Profiles
import com.gymwatch.core.domain.model.WorkoutProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesUseCaseTest {

    private class Fixture(initial: Profiles = Profiles.DEFAULT, healthAppInstalled: Boolean = true) {
        val scope = TestScope()
        val repository = InMemoryProfilesRepository(initial)
        val haptics = RecordingHaptics()
        val healthApp = FakeHealthApp(available = healthAppInstalled)
        val useCase = ProfilesUseCase(repository, healthApp, haptics, scope)
    }

    @Test
    fun `selecting a profile persists the selection`() = runTest {
        val f = Fixture()
        f.useCase.select(2)
        f.scope.runCurrent()

        assertEquals(2, f.repository.saved?.selected)
        assertEquals(ExerciseKind.CYCLING, f.useCase.state.value.current.kind)
    }

    @Test
    fun `selecting the already active profile does not write or buzz`() = runTest {
        val f = Fixture()
        f.useCase.select(0)
        f.scope.runCurrent()

        assertEquals(null, f.repository.saved)
        assertTrue(f.haptics.played.isEmpty(), "no feedback for a no-op")
    }

    @Test
    fun `replacing a profile keeps the others and the selection`() = runTest {
        val f = Fixture(Profiles.DEFAULT.select(2))
        val replacement = WorkoutProfile(ExerciseKind.ROWING, counterLabel = CounterLabel.ROUNDS)

        f.useCase.setProfileAt(1, replacement)
        f.scope.runCurrent()

        val saved = requireNotNull(f.repository.saved)
        assertEquals(replacement, saved.entries[1])
        assertEquals(Profiles.DEFAULT.entries[0], saved.entries[0])
        assertEquals(2, saved.selected, "editing a slot must not move the selection")
    }

    @Test
    fun `setting one rest preset leaves the other two alone`() = runTest {
        val f = Fixture()
        f.useCase.setRestPreset(profileIndex = 0, presetIndex = 1, duration = 75.seconds)
        f.scope.runCurrent()

        val presets = requireNotNull(f.repository.saved).entries[0].restPresets
        assertEquals(75.seconds, presets[1])
        assertEquals(Profiles.DEFAULT.entries[0].restPresets[0], presets[0])
        assertEquals(Profiles.DEFAULT.entries[0].restPresets[2], presets[2])
    }

    @Test
    fun `a write reads the repository rather than the seeded StateFlow`() = runTest {
        // The StateFlow seeds with Profiles.DEFAULT (selected = 0). A tap that
        // lands before the first stored emission must still transform the stored
        // value, which here already has slot 2 selected. See docs/LESSONS.md #10.
        val f = Fixture(Profiles.DEFAULT.select(2))

        f.useCase.setRestPreset(profileIndex = 2, presetIndex = 0, duration = 20.seconds)
        f.scope.runCurrent()

        val saved = requireNotNull(f.repository.saved)
        assertEquals(2, saved.selected)
        assertEquals(20.seconds, saved.entries[2].restPresets[0])
    }

    @Test
    fun `opening the health app reports failure when it is missing`() = runTest {
        val f = Fixture(healthAppInstalled = false)

        assertFalse(f.useCase.isHealthAppAvailable())
        assertFalse(f.useCase.openHealthApp())
        assertEquals(0, f.healthApp.openCount)
    }

    @Test
    fun `opening the health app launches it once`() = runTest {
        val f = Fixture()

        assertTrue(f.useCase.openHealthApp())
        assertEquals(1, f.healthApp.openCount)
    }
}
