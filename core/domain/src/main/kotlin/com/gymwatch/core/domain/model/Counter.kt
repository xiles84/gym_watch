package com.gymwatch.core.domain.model

/**
 * The set counter. Floors at zero — a negative count is never what the user
 * meant, and silently clamping beats showing "-1" mid-workout.
 *
 * One count, zeroed by hand when moving to the next machine. That is the whole
 * workflow, which is why there is no count per exercise and no label to choose.
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
