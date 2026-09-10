package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Chronometer
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.ResetOutcome
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.HapticsPort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

/**
 * Holds chronometer state. Note there is no ticker here: the state is a start
 * mark, and the *display* derives elapsed time from the clock whenever it
 * renders. Nothing needs to run for the value to stay correct.
 */
class ChronometerUseCase(
    private val clock: ClockPort,
    private val haptics: HapticsPort,
) {
    private val _state = MutableStateFlow(Chronometer.Idle)
    val state: StateFlow<Chronometer> = _state.asStateFlow()

    fun toggle() {
        _state.update { it.toggle(clock.elapsed()) }
        haptics.play(Haptic.CONFIRM)
    }

    fun lap() {
        val before = _state.value
        _state.update { it.lap(clock.elapsed()) }
        if (_state.value.laps.size != before.laps.size) haptics.play(Haptic.TICK)
    }

    /**
     * Resets at once when paused or idle. A running chronometer is a set being
     * timed, so it asks first and changes nothing — not even a buzz, since
     * nothing has happened yet.
     */
    fun requestReset(): ResetOutcome {
        if (_state.value.needsResetConfirmation) return ResetOutcome.NEEDS_CONFIRMATION
        confirmReset()
        return ResetOutcome.DONE
    }

    /** Unconditional: the user has already said yes, or there was nothing to ask. */
    fun confirmReset() {
        _state.update { it.reset() }
        haptics.play(Haptic.CONFIRM)
    }

    /** Elapsed time right now — what the UI renders each frame. */
    fun elapsedNow(): Duration = _state.value.elapsedAt(clock.elapsed())

    fun currentLapNow(): Duration = _state.value.currentLapAt(clock.elapsed())
}
