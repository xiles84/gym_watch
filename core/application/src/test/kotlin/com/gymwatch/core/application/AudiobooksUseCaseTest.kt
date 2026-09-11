package com.gymwatch.core.application

import com.gymwatch.core.application.fake.FakeAudiobookPlayer
import com.gymwatch.core.application.fake.FakeMediaApps
import com.gymwatch.core.application.fake.InMemoryRecentAudiobooksRepository
import com.gymwatch.core.application.fake.RecordingHaptics
import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.MediaApp
import com.gymwatch.core.domain.model.PlayOutcome
import com.gymwatch.core.domain.model.RecentAudiobooks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AudiobooksUseCaseTest {

    private val faire = Audiobook("The Case of the Felonious Faire")

    @Test
    fun `the list is the stored one`() = runTest {
        val stored = RecentAudiobooks.EMPTY.listened(faire)
        val useCase = AudiobooksUseCase(
            InMemoryRecentAudiobooksRepository(stored),
            FakeAudiobookPlayer(),
            RecordingHaptics(),
            backgroundScope,
        )
        runCurrent()

        assertEquals(stored, useCase.recent.value)
    }

    @Test
    fun `playing asks for the book by title and buzzes once the phone accepts`() = runTest {
        val player = FakeAudiobookPlayer(PlayOutcome.STARTING)
        val haptics = RecordingHaptics()
        val useCase = AudiobooksUseCase(InMemoryRecentAudiobooksRepository(), player, haptics, backgroundScope)

        val outcome = useCase.play(faire)

        assertEquals(PlayOutcome.STARTING, outcome)
        assertEquals(listOf("The Case of the Felonious Faire"), player.requested)
        assertEquals(listOf(Haptic.CONFIRM), haptics.played)
    }

    @Test
    fun `a request the phone could not act on does not buzz`() = runTest {
        val haptics = RecordingHaptics()
        val useCase = AudiobooksUseCase(
            InMemoryRecentAudiobooksRepository(),
            FakeAudiobookPlayer(PlayOutcome.PHONE_UNREACHABLE),
            haptics,
            backgroundScope,
        )

        assertEquals(PlayOutcome.PHONE_UNREACHABLE, useCase.play(faire))
        assertTrue(haptics.played.isEmpty())
    }

    @Test
    fun `shortcuts list only installed apps and buzz when one opens`() = runTest {
        val apps = FakeMediaApps(installed = setOf(MediaApp.SPOTIFY))
        val haptics = RecordingHaptics()
        val shortcuts = MediaShortcutsUseCase(apps, haptics)

        assertEquals(setOf(MediaApp.SPOTIFY), shortcuts.installed())
        assertTrue(shortcuts.open(MediaApp.SPOTIFY))
        assertFalse(shortcuts.open(MediaApp.PHONE_CONTROLS))
        assertEquals(listOf(MediaApp.SPOTIFY), apps.opened)
        assertEquals(listOf(Haptic.CONFIRM), haptics.played)
    }
}
