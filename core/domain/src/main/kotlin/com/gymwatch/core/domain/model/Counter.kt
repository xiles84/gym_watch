package com.gymwatch.core.domain.model

/**
 * The set/rep counter. Floors at zero — a negative rep count is never what the
 * user meant, and silently clamping beats showing "-1" mid-workout.
 */
data class Counter(
    val value: Int = 0,
    val label: String = DEFAULT_LABEL,
) {
    fun increment(): Counter = copy(value = (value + 1).coerceAtMost(MAX))

    fun decrement(): Counter = copy(value = (value - 1).coerceAtLeast(MIN))

    fun stepBy(delta: Int): Counter = copy(value = (value + delta).coerceIn(MIN, MAX))

    fun reset(): Counter = copy(value = 0)

    fun withLabel(new: String): Counter =
        copy(label = new.trim().take(MAX_LABEL_LENGTH).ifEmpty { DEFAULT_LABEL })

    companion object {
        const val MIN = 0
        const val MAX = 999
        const val MAX_LABEL_LENGTH = 16
        const val DEFAULT_LABEL = "SETS"
    }
}
