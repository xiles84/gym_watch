package com.gymwatch.core.domain.model

import kotlin.time.Duration

/**
 * What the user configures, and there is exactly one of it.
 *
 * This used to be three switchable profiles, each with its own rest lengths and
 * counter label. One configuration was all that got used: the counter is zeroed
 * by hand between machines, so a label per workout bought nothing. What earned
 * its place is the three shortcuts, each of which opens that exercise in the
 * watch's health app — which records it, since nothing else can
 * (docs/LESSONS.md #2).
 */
data class WorkoutSetup(
    val shortcuts: List<ExerciseKind>,
    val restPresets: RestPresets = RestPresets.DEFAULT,
) {

    /** Safe by index: a corrupt store must not be able to throw here. */
    fun shortcutAt(index: Int): ExerciseKind =
        shortcuts.getOrNull(index) ?: DEFAULT.shortcuts[0]

    fun withShortcutAt(index: Int, kind: ExerciseKind): WorkoutSetup =
        if (index !in shortcuts.indices) this
        else copy(shortcuts = shortcuts.toMutableList().also { it[index] = kind })

    /** Convenience for the preset editor, which only ever changes one duration. */
    fun withRestPresetAt(index: Int, duration: Duration): WorkoutSetup =
        copy(restPresets = restPresets.withDurationAt(index, duration))

    companion object {
        /** Three thumb-sized circles is what fits in one row on a round screen. */
        const val SHORTCUT_COUNT = 3

        val DEFAULT = WorkoutSetup(
            shortcuts = listOf(
                ExerciseKind.WEIGHT_MACHINE,
                ExerciseKind.TREADMILL,
                ExerciseKind.EXERCISE_BIKE,
            ),
            restPresets = RestPresets.DEFAULT,
        )

        /**
         * The only way persisted data should enter. Pads and trims to exactly
         * [SHORTCUT_COUNT], so stored data of the wrong shape degrades instead of
         * throwing.
         */
        fun of(shortcuts: List<ExerciseKind>, restPresets: RestPresets): WorkoutSetup =
            WorkoutSetup(
                shortcuts = List(SHORTCUT_COUNT) { index ->
                    shortcuts.getOrNull(index) ?: DEFAULT.shortcuts[index]
                },
                restPresets = restPresets,
            )
    }
}
