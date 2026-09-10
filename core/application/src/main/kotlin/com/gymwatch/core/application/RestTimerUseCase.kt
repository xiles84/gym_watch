package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.RestPresets
import com.gymwatch.core.domain.model.RestTimer
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.ProfilesRepositoryPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Countdown between sets, started from one of the active profile's three
 * presets.
 *
 * The watchdog sleeps for exactly the remaining time and then re-checks against
 * the clock rather than counting down in fixed steps — so a long doze cannot
 * make it fire late or twice.
 */
class RestTimerUseCase(
    private val clock: ClockPort,
    private val profiles: ProfilesRepositoryPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RestTimer())
    val state: StateFlow<RestTimer> = _state.asStateFlow()

    /** The active profile's presets — what the three buttons show. */
    val presets: StateFlow<RestPresets> =
        profiles.profiles
            .map { it.current.restPresets }
            .stateIn(scope, SharingStarted.Eagerly, RestPresets.DEFAULT)

    private var watchdog: Job? = null

    /**
     * Starts (or restarts) the countdown on preset [index].
     *
     * The duration is read from the repository rather than from [presets],
     * whose seeded default would otherwise be used by a tap that lands before
     * the first stored emission (docs/LESSONS.md #10).
     */
    fun start(index: Int) {
        scope.launch {
            val duration = profiles.profiles.first().current.restPresets[index]
            startFor(duration)
        }
    }

    fun cancel() {
        watchdog?.cancel()
        watchdog = null
        _state.update { it.cancel() }
    }

    fun remainingNow(): Duration = _state.value.remainingAt(clock.elapsed())

    fun progressNow(): Float = _state.value.progressAt(clock.elapsed())

    private fun startFor(duration: Duration) {
        // withDuration clamps to RestTimer's own bounds and clears any old mark.
        _state.value = RestTimer().withDuration(duration).start(clock.elapsed())
        watchdog?.cancel()
        watchdog = scope.launch {
            while (isActive) {
                val snapshot = _state.value
                if (!snapshot.isRunning) break
                val remaining = snapshot.remainingAt(clock.elapsed())
                if (remaining == Duration.ZERO) {
                    haptics.play(Haptic.REST_OVER)
                    _state.update { it.cancel() }
                    break
                }
                delay(remaining)
            }
        }
    }
}
