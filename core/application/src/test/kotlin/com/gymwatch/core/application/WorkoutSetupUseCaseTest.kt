package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeHealthApp
import com.gymwatch.core.application.fake.InMemoryWorkoutSetupRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.WorkoutSetup
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
class WorkoutSetupUseCaseTest {

    private class Fixture(
        initial: WorkoutSetup = WorkoutSetup.DEFAULT,
        healthAppInstalled: Boolean = true,
        directRouteWorks: Boolean = true,
    ) {
        val scope = TestScope()
        val repository = InMemoryWorkoutSetupRepository(initial)
        val haptics = RecordingHaptics()
        val healthApp = FakeHealthApp(healthAppInstalled, directRouteWorks)
        val useCase = WorkoutSetupUseCase(repository, healthApp, haptics, scope)
    }

    @Test
    fun `setting a shortcut persists it and keeps the others`() = runTest {
        val f = Fixture()
        f.useCase.setShortcut(2, ExerciseKind.DEADLIFT)
        f.scope.runCurrent()

        val saved = requireNotNull(f.repository.saved)
        assertEquals(ExerciseKind.DEADLIFT, saved.shortcuts[2])
        assertEquals(WorkoutSetup.DEFAULT.shortcuts.take(2), saved.shortcuts.take(2))
    }

    @Test
    fun `setting a shortcut to what it already is does not write or buzz`() = runTest {
        val f = Fixture()
        f.useCase.setShortcut(0, WorkoutSetup.DEFAULT.shortcuts[0])
        f.scope.runCurrent()

        assertEquals(null, f.repository.saved)
        assertTrue(f.haptics.played.isEmpty(), "no feedback for a no-op")
    }

    @Test
    fun `setting one rest preset leaves the other two alone`() = runTest {
        val f = Fixture()
        f.useCase.setRestPreset(index = 1, duration = 75.seconds)
        f.scope.runCurrent()

        val presets = requireNotNull(f.repository.saved).restPresets
        assertEquals(75.seconds, presets[1])
        assertEquals(WorkoutSetup.DEFAULT.restPresets[0], presets[0])
        assertEquals(WorkoutSetup.DEFAULT.restPresets[2], presets[2])
    }

    @Test
    fun `a write reads the repository rather than the seeded StateFlow`() = runTest {
        // The StateFlow seeds with WorkoutSetup.DEFAULT. A tap that lands before
        // the first stored emission must still transform the stored value, whose
        // third shortcut is Yoga. See docs/LESSONS.md #10.
        val stored = WorkoutSetup.DEFAULT.withShortcutAt(2, ExerciseKind.YOGA)
        val f = Fixture(stored)

        f.useCase.setRestPreset(index = 0, duration = 20.seconds)
        f.scope.runCurrent()

        val saved = requireNotNull(f.repository.saved)
        assertEquals(ExerciseKind.YOGA, saved.shortcuts[2])
        assertEquals(20.seconds, saved.restPresets[0])
    }

    @Test
    fun `starting a shortcut opens the health app on that exercise`() = runTest {
        val f = Fixture()

        assertTrue(f.useCase.startWorkout(1))
        assertEquals(listOf(WorkoutSetup.DEFAULT.shortcuts[1]), f.healthApp.started)
        assertEquals(0, f.healthApp.openCount, "no need for the home screen")
        assertEquals(listOf(Haptic.CONFIRM), f.haptics.played)
    }

    @Test
    fun `starting reads the stored shortcut, not the seeded default`() = runTest {
        val f = Fixture(WorkoutSetup.DEFAULT.withShortcutAt(0, ExerciseKind.ROWING_MACHINE))

        f.useCase.startWorkout(0)

        assertEquals(listOf(ExerciseKind.ROWING_MACHINE), f.healthApp.started)
    }

    @Test
    fun `when the direct route is gone it falls back to the home screen`() = runTest {
        val f = Fixture(directRouteWorks = false)

        assertTrue(f.useCase.startWorkout(0))
        assertEquals(1, f.healthApp.started.size, "the direct route is still tried first")
        assertEquals(1, f.healthApp.openCount)
    }

    @Test
    fun `with no health app installed starting reports failure and stays silent`() = runTest {
        val f = Fixture(healthAppInstalled = false)

        assertFalse(f.useCase.startWorkout(0))
        assertTrue(f.haptics.played.isEmpty())
    }
}
