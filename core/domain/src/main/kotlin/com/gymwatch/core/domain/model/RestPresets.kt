package com.gymwatch.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The three rest lengths offered on the rest screen, one tap each.
 *
 * Three, because that is what fits as thumb-sized targets on a round screen
 * without scrolling — the same reasoning that caps [WorkoutSetup.SHORTCUT_COUNT].
 *
 * Every duration is clamped to [RestTimer]'s own bounds on the way in, so a
 * preset can never hold a value the timer would refuse to run.
 */
data class RestPresets(val durations: List<Duration>) {

    /** Safe by index: a corrupt store must not be able to throw here. */
    operator fun get(index: Int): Duration =
        durations.getOrNull(index) ?: DEFAULT.durations[0]

    fun withDurationAt(index: Int, duration: Duration): RestPresets =
        if (index !in durations.indices) this
        else RestPresets(durations.toMutableList().also { it[index] = clamp(duration) })

    companion object {
        const val COUNT = 3

        val DEFAULT = of(listOf(60.seconds, 90.seconds, 2.minutes))

        /**
         * The only way persisted data should enter. Pads, trims and clamps, so
         * a stored list of the wrong length or with silly values still yields a
         * usable object instead of crashing mid-workout.
         */
        fun of(durations: List<Duration>): RestPresets {
            val padded = List(COUNT) { index ->
                durations.getOrNull(index) ?: FALLBACKS[index]
            }
            return RestPresets(padded.map(::clamp))
        }

        private val FALLBACKS = listOf(60.seconds, 90.seconds, 2.minutes)

        private fun clamp(duration: Duration): Duration =
            duration.coerceIn(RestTimer.MIN, RestTimer.MAX)
    }
}
