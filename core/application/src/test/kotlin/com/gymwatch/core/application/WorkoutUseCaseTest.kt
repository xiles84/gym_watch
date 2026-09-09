package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeWorkoutSession
import com.gymwatch.core.application.fake.InMemoryFavouritesRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Favourites
import com.gymwatch.core.domain.model.SessionOwnership
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutUseCaseTest {

    @Test
    fun `starts immediately when nobody owns the exercise slot`() = runTest {
        val session = FakeWorkoutSession(ownership = SessionOwnership.NONE)
        val useCase = WorkoutUseCase(
            session, InMemoryFavouritesRepository(), RecordingHaptics(), backgroundScope,
        )

        val outcome = useCase.requestStart(ExerciseKind.WEIGHTS)

        assertIs<StartOutcome.Started>(outcome)
        assertEquals(listOf(ExerciseKind.WEIGHTS), session.startedKinds)
    }

    @Test
    fun `asks first when another app owns the slot and does NOT start`() = runTest {
        val session = FakeWorkoutSession(ownership = SessionOwnership.OTHER_APP)
        val useCase = WorkoutUseCase(
            session, InMemoryFavouritesRepository(), RecordingHaptics(), backgroundScope,
        )

        val outcome = useCase.requestStart(ExerciseKind.TREADMILL)

        assertIs<StartOutcome.NeedsConfirmation>(outcome)
        assertTrue(session.startedKinds.isEmpty(), "must not hijack Samsung Health silently")
    }

    @Test
    fun `forceStart takes the slot once the user has confirmed`() = runTest {
        val session = FakeWorkoutSession(ownership = SessionOwnership.OTHER_APP)
        val useCase = WorkoutUseCase(
            session, InMemoryFavouritesRepository(), RecordingHaptics(), backgroundScope,
        )

        useCase.requestStart(ExerciseKind.TREADMILL)
        useCase.forceStart(ExerciseKind.TREADMILL)

        assertEquals(listOf(ExerciseKind.TREADMILL), session.startedKinds)
    }

    @Test
    fun `reports AlreadyRunning instead of restarting our own session`() = runTest {
        val session = FakeWorkoutSession(ownership = SessionOwnership.OURS)
        val useCase = WorkoutUseCase(
            session, InMemoryFavouritesRepository(), RecordingHaptics(), backgroundScope,
        )

        assertIs<StartOutcome.AlreadyRunning>(useCase.requestStart(ExerciseKind.WEIGHTS))
        assertTrue(session.startedKinds.isEmpty())
    }

    @Test
    fun `refuses a kind this watch cannot track`() = runTest {
        val session = FakeWorkoutSession(supported = setOf(ExerciseKind.RUNNING))
        val useCase = WorkoutUseCase(
            session, InMemoryFavouritesRepository(), RecordingHaptics(), backgroundScope,
        )

        assertIs<StartOutcome.Unsupported>(useCase.requestStart(ExerciseKind.ROWING))
    }

    @Test
    fun `toggling favourites respects the cap of three`() = runTest {
        val repo = InMemoryFavouritesRepository(Favourites.DEFAULT)
        val useCase = WorkoutUseCase(
            FakeWorkoutSession(), repo, RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        useCase.toggleFavourite(ExerciseKind.ROWING)   // already full — ignored
        runCurrent()
        assertEquals(Favourites.DEFAULT, useCase.favourites.value)

        useCase.toggleFavourite(ExerciseKind.WEIGHTS)  // remove one
        runCurrent()
        useCase.toggleFavourite(ExerciseKind.ROWING)   // now it fits
        runCurrent()

        assertTrue(ExerciseKind.ROWING in useCase.favourites.value.kinds)
        assertEquals(Favourites.MAX, useCase.favourites.value.kinds.size)
    }

    @Test
    fun `ending clears the session snapshot`() = runTest {
        val session = FakeWorkoutSession()
        val useCase = WorkoutUseCase(
            session, InMemoryFavouritesRepository(), RecordingHaptics(), backgroundScope,
        )
        runCurrent()

        useCase.requestStart(ExerciseKind.WEIGHTS)
        runCurrent()
        useCase.end()
        runCurrent()

        assertEquals(null, useCase.snapshot.value)
    }
}
