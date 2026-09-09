package com.gymwatch.core.application

import com.gymwatch.core.domain.model.RestTimer
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.RestTimerSettingsPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Countdown between sets.
 *
 * The watchdog sleeps for exactly the remaining time and then re-checks against
 * the clock rather than counting down in fixed steps — so a long doze cannot
 * make it fire late or twice.
 */
class RestTimerUseCase(
    private val clock: ClockPort,
    private val settings: RestTimerSettingsPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RestTimer())
    val state: StateFlow<RestTimer> = _state.asStateFlow()

    private var watchdog: Job? = null

    init {
        scope.launch {
            settings.duration.collect { saved ->
                // Only adopt a new configured length while idle; never yank the
                // rug out from under a running countdown.
                _state.update { if (it.isRunning) it else it.withDuration(saved) }
            }
        }
    }

    fun start() {
        _state.update { it.start(clock.elapsed()) }
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

    fun cancel() {
        watchdog?.cancel()
        watchdog = null
        _state.update { it.cancel() }
    }

    fun toggle() = if (_state.value.isRunning) cancel() else start()

    /** Rotary bezel adjusts the configured rest length; persisted for next time. */
    fun adjustBy(delta: Duration) {
        val next = _state.value.adjustBy(delta)
        if (next.duration == _state.value.duration) return
        _state.value = next
        haptics.play(Haptic.TICK)
        scope.launch { settings.save(next.duration) }
    }

    fun remainingNow(): Duration = _state.value.remainingAt(clock.elapsed())

    fun progressNow(): Float = _state.value.progressAt(clock.elapsed())
}
