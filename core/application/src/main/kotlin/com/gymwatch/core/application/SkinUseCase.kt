package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.Skin
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.SkinRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The colour scheme the whole app is drawn in.
 *
 * One skin for the whole app, chosen in settings. It is how the watch looks,
 * not part of any workout, so nothing mid-session ever repaints it.
 */
class SkinUseCase(
    private val repository: SkinRepositoryPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    /** Display only — never the basis of a write. See [select]. */
    val state: StateFlow<Skin> =
        repository.skin.stateIn(scope, SharingStarted.Eagerly, Skin.DEFAULT)

    /**
     * Reads the repository rather than [state], which is seeded with
     * [Skin.DEFAULT] until the first emission arrives (docs/LESSONS.md #10).
     * Here that only decides whether the haptic fires, but the rule is the rule:
     * a read that feeds a write goes to the source.
     */
    fun select(skin: Skin) {
        scope.launch {
            if (repository.skin.first() == skin) return@launch
            haptics.play(Haptic.TICK)
            repository.save(skin)
        }
    }
}
