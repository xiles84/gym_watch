package com.gymwatch.core.application

import com.gymwatch.core.domain.model.ExerciseKind
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.WorkoutSetup
import com.gymwatch.core.domain.port.CompanionHealthAppPort
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.WorkoutSetupRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * The three workout shortcuts, the rest presets, and the hand-off to the app
 * that actually records workouts.
 *
 * This app deliberately does **not** track exercises itself. Health Services
 * would give us live metrics but persists nothing, and there is no way to get a
 * record into Samsung Health from a watch app — while starting our own exercise
 * would *end* whatever Samsung Health was recording, because the platform allows
 * exactly one device-wide. See docs/LESSONS.md #2 and #3.
 *
 * So a shortcut opens Samsung Health on that exercise's start screen, and the
 * user presses start there.
 */
class WorkoutSetupUseCase(
    private val repository: WorkoutSetupRepositoryPort,
    private val healthApp: CompanionHealthAppPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    /** Display only — never the basis of a write. See [mutate]. */
    val state: StateFlow<WorkoutSetup> =
        repository.setup.stateIn(scope, SharingStarted.Eagerly, WorkoutSetup.DEFAULT)

    fun setShortcut(index: Int, kind: ExerciseKind) =
        mutate(Haptic.CONFIRM) { it.withShortcutAt(index, kind) }

    /** Used by the rest preset editor, which only ever changes one duration. */
    fun setRestPreset(index: Int, duration: Duration) =
        mutate(Haptic.TICK) { it.withRestPresetAt(index, duration) }

    /**
     * Opens the health app on shortcut [index]'s exercise — or on its home
     * screen if the direct route is gone. That route is undocumented and an app
     * update can remove it; landing one tap away beats a button that does
     * nothing.
     *
     * Reads the repository rather than [state], for the same reason [mutate]
     * does: a tap right after launch must open the stored exercise, not the
     * seeded default.
     */
    suspend fun startWorkout(index: Int): Boolean {
        val kind = repository.setup.first().shortcutAt(index)
        val opened = healthApp.startWorkout(kind) || healthApp.open()
        if (opened) haptics.play(Haptic.CONFIRM)
        return opened
    }

    /**
     * Reads the *repository*, not [state], before transforming.
     *
     * The cached StateFlow starts on a fabricated default and is only replaced
     * once the stored value arrives, so a tap landing before that first emission
     * would transform the default and write it back over real data. Always
     * transform the value the store actually holds (docs/LESSONS.md #10).
     */
    private fun mutate(haptic: Haptic?, transform: (WorkoutSetup) -> WorkoutSetup) {
        scope.launch {
            val current = repository.setup.first()
            val next = transform(current)
            if (next == current) return@launch
            haptic?.let(haptics::play)
            repository.save(next)
        }
    }
}
