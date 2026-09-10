package com.gymwatch.core.application

import com.gymwatch.core.domain.model.AppScreen
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.ScreenLayout
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.ScreenLayoutRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Which screens appear in the pager, and in what order.
 *
 * The settings screen itself is not represented here — the UI appends it as the
 * last page unconditionally, so it cannot be hidden by the very screen that
 * hides things.
 */
class ScreenLayoutUseCase(
    private val repository: ScreenLayoutRepositoryPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    /** Display only — never the basis of a write. See [mutate]. */
    val state: StateFlow<ScreenLayout> =
        repository.layout.stateIn(scope, SharingStarted.Eagerly, ScreenLayout.DEFAULT)

    /**
     * Hiding the last visible screen is refused by the domain, so this quietly
     * does nothing rather than leaving the pager with no pages.
     */
    fun toggle(screen: AppScreen) = mutate(Haptic.TICK) { it.toggle(screen) }

    fun move(screen: AppScreen, delta: Int) = mutate(Haptic.TICK) { it.move(screen, delta) }

    /** Reads the repository, not the seeded StateFlow (docs/LESSONS.md #10). */
    private fun mutate(haptic: Haptic?, transform: (ScreenLayout) -> ScreenLayout) {
        scope.launch {
            val current = repository.layout.first()
            val next = transform(current)
            if (next == current) return@launch
            haptic?.let(haptics::play)
            repository.save(next)
        }
    }
}
