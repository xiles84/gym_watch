package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Audiobook
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.PlayOutcome
import com.gymwatch.core.domain.model.RecentAudiobooks
import com.gymwatch.core.domain.port.AudiobookPlayerPort
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.RecentAudiobooksRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * The recently listened audiobooks, and starting one of them on the phone.
 *
 * The phone plays; the watch only asks. Audible's watch app can play only on the
 * watch, and there's no other way to point the phone at a particular book
 * (docs/LESSONS.md #32).
 */
class AudiobooksUseCase(
    repository: RecentAudiobooksRepositoryPort,
    private val player: AudiobookPlayerPort,
    private val haptics: HapticsPort,
    scope: CoroutineScope,
) {
    /** Display only. Nothing here writes, so the seeded empty list is harmless. */
    val recent: StateFlow<RecentAudiobooks> =
        repository.recent.stateIn(scope, SharingStarted.Eagerly, RecentAudiobooks.EMPTY)

    /** Buzzes only once the phone has accepted the request, not merely when the tap lands. */
    suspend fun play(book: Audiobook): PlayOutcome {
        val outcome = player.play(book.title)
        if (outcome == PlayOutcome.STARTING) haptics.play(Haptic.CONFIRM)
        return outcome
    }
}
