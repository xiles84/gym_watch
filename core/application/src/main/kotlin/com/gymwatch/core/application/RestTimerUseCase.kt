package com.gymwatch.core.application

import com.gymwatch.core.domain.model.Haptic
import com.gymwatch.core.domain.model.ResetOutcome
import com.gymwatch.core.domain.model.RestPresets
import com.gymwatch.core.domain.model.RestTimer
import com.gymwatch.core.domain.port.ClockPort
import com.gymwatch.core.domain.port.HapticsPort
import com.gymwatch.core.domain.port.WorkoutSetupRepositoryPort
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
import kotlin.time.Duration.Companion.seconds

/**
 * Countdown between sets, started from one of the three presets, and the alarm
 * that follows it.
 *
 * At zero the timer does not drop back to idle on its own. It rings —
 * [Haptic.REST_OVER] every [ALARM_REPEAT] — until reset, because one buzz is
 * easy to miss and the whole point is not to over-rest.
 *
 * The watchdog sleeps for exactly the remaining time and then re-checks against
 * the clock rather than counting down in fixed steps, so a long doze cannot make
 * the alarm start late. The repeat is a plain delay on purpose: after a doze it
 * carries on with one buzz rather than replaying every buzz it slept through.
 */
class RestTimerUseCase(
    private val clock: ClockPort,
    private val setup: WorkoutSetupRepositoryPort,
    private val haptics: HapticsPort,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(RestTimer())
    val state: StateFlow<RestTimer> = _state.asStateFlow()

    private val _alarming = MutableStateFlow(false)

    /**
     * True from zero until reset. A flow of its own because reaching zero is not
     * a change to [state] — the start mark stays put — and the watch-face
     * indicator needs something to observe to say "Rest over".
     */
    val alarming: StateFlow<Boolean> = _alarming.asStateFlow()

    /** The stored presets — what the three buttons show. */
    val presets: StateFlow<RestPresets> =
        setup.setup
            .map { it.restPresets }
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
            val duration = setup.setup.first().restPresets[index]
            startFor(duration)
        }
    }

    /**
     * Resets at once unless a countdown is in progress, which asks first and
     * changes nothing. Idle and ringing both reset straight away: there is
     * nothing to lose, and at zero the reset is what silences the alarm.
     */
    fun requestReset(): ResetOutcome {
        if (_state.value.needsResetConfirmationAt(clock.elapsed())) {
            return ResetOutcome.NEEDS_CONFIRMATION
        }
        confirmReset()
        return ResetOutcome.DONE
    }

    /** Unconditional. Stopping the watchdog is what stops the buzzing. */
    fun confirmReset() {
        watchdog?.cancel()
        watchdog = null
        _alarming.value = false
        _state.update { it.cancel() }
    }

    fun remainingNow(): Duration = _state.value.remainingAt(clock.elapsed())

    fun progressNow(): Float = _state.value.progressAt(clock.elapsed())

    /**
     * Read from the clock, like [remainingNow], so the screen switches to the
     * alarm on the frame the countdown reaches zero rather than when the
     * watchdog next wakes.
     */
    fun isAlarmingNow(): Boolean = _state.value.isAlarmingAt(clock.elapsed())

    fun needsResetConfirmationNow(): Boolean =
        _state.value.needsResetConfirmationAt(clock.elapsed())

    private fun startFor(duration: Duration) {
        // withDuration clamps to RestTimer's own bounds and clears any old mark.
        _alarming.value = false
        _state.value = RestTimer().withDuration(duration).start(clock.elapsed())
        watchdog?.cancel()
        watchdog = scope.launch {
            while (isActive) {
                val snapshot = _state.value
                if (!snapshot.isRunning) break
                val remaining = snapshot.remainingAt(clock.elapsed())
                if (remaining > Duration.ZERO) {
                    delay(remaining)
                    continue
                }
                _alarming.value = true
                haptics.play(Haptic.REST_OVER)
                delay(ALARM_REPEAT)
            }
        }
    }

    companion object {
        /** The buzz pattern lasts 1.2 s, so 3 s leaves a clear gap between repeats. */
        val ALARM_REPEAT: Duration = 3.seconds
    }
}
