package com.gymwatch.core.domain.model

/**
 * The set/rep counter. Floors at zero — a negative rep count is never what the
 * user meant, and silently clamping beats showing "-1" mid-workout.
 *
 * What it is *called* is not stored here: that comes from the active
 * [WorkoutProfile], so switching from weights to a run relabels it without
 * touching the count.
 */
data class Counter(val value: Int = 0) {

    fun increment(): Counter = copy(value = (value + 1).coerceAtMost(MAX))

    fun decrement(): Counter = copy(value = (value - 1).coerceAtLeast(MIN))

    fun stepBy(delta: Int): Counter = copy(value = (value + delta).coerceIn(MIN, MAX))

    fun reset(): Counter = copy(value = 0)

    companion object {
        const val MIN = 0
        const val MAX = 999
    }
}
