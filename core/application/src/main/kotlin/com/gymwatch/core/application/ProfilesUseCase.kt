package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.Profiles
import com.gymwatch.core.domain.model.WorkoutProfile
import com.gymwatch.core.domain.port.CompanionHealthAppPort
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.ProfilesRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * The three profiles, and the hand-off to the app that actually records
 * workouts.
 *
 * This app deliberately does **not** track exercises itself. Health Services
 * would give us live metrics but persists nothing, and there is no way to get a
 * record into Samsung Health from a watch app — while starting our own exercise
 * would *end* whatever Samsung Health was recording, because the platform allows
 * exactly one device-wide. See docs/LESSONS.md #2 and #3.
 */
class ProfilesUseCase(
    private val repository: ProfilesRepositoryPort,
    private val healthApp: CompanionHealthAppPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    /** Display only — never the basis of a write. See [mutate]. */
    val state: StateFlow<Profiles> =
        repository.profiles.stateIn(scope, SharingStarted.Eagerly, Profiles.DEFAULT)

    fun select(index: Int) = mutate(Haptic.CONFIRM) { it.select(index) }

    fun setProfileAt(index: Int, profile: WorkoutProfile) =
        mutate(Haptic.CONFIRM) { it.replaceAt(index, profile) }

    /** Used by the rest preset editor, which only ever changes one duration. */
    fun setRestPreset(profileIndex: Int, presetIndex: Int, duration: Duration) =
        mutate(Haptic.TICK) { it.withRestPresetAt(profileIndex, presetIndex, duration) }

    suspend fun isHealthAppAvailable(): Boolean = healthApp.isAvailable()

    suspend fun openHealthApp(): Boolean {
        val opened = healthApp.open()
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
    private fun mutate(haptic: Haptic?, transform: (Profiles) -> Profiles) {
        scope.launch {
            val current = repository.profiles.first()
            val next = transform(current)
            if (next == current) return@launch
            haptic?.let(haptics::play)
            repository.save(next)
        }
    }
}
