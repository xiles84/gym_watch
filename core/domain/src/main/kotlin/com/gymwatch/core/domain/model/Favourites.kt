package com.gymwatch.core.domain.model

/**
 * The pinned quick-start activities. Capped at [MAX] because that is what fits
 * on a round screen without scrolling — the cap is a UI truth expressed in the
 * domain so no adapter has to re-enforce it.
 */
data class Favourites(val kinds: List<ExerciseKind>) {

    val isFull: Boolean get() = kinds.size >= MAX

    fun toggle(kind: ExerciseKind): Favourites = when {
        kind in kinds -> Favourites(kinds - kind)
        isFull -> this            // silently ignored; UI disables the row instead
        else -> Favourites(kinds + kind)
    }

    companion object {
        const val MAX = 3

        val DEFAULT = Favourites(
            listOf(ExerciseKind.WEIGHTS, ExerciseKind.TREADMILL, ExerciseKind.CYCLING),
        )

        /** Trims anything over the cap and drops duplicates. */
        fun of(kinds: List<ExerciseKind>): Favourites =
            Favourites(kinds.distinct().take(MAX))
    }
}
