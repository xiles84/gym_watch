package com.gymwatch.core.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * One saved way of using the app.
 *
 * A profile does **not** record a workout — Samsung Health does that, and no
 * public API lets a third-party watch app write into it (see docs/LESSONS.md
 * #2). What a profile does is reconfigure the parts we *can* own: how long you
 * rest between sets, and what the counter is counting.
 *
 * The identity is the [kind], so there is nothing to type.
 */
data class WorkoutProfile(
    val kind: ExerciseKind,
    val restPresets: RestPresets = RestPresets.DEFAULT,
    val counterLabel: CounterLabel = CounterLabel.SETS,
)

/**
 * The three profiles and which one is active.
 *
 * Capped at [COUNT] for the same reason the old favourites list was: three
 * full-width pills is what a round screen holds without scrolling.
 */
data class Profiles(val entries: List<WorkoutProfile>, val selected: Int = 0) {

    val current: WorkoutProfile
        get() = entries.getOrNull(selected) ?: entries.first()

    fun select(index: Int): Profiles =
        if (index !in entries.indices) this else copy(selected = index)

    fun replaceAt(index: Int, profile: WorkoutProfile): Profiles =
        if (index !in entries.indices) this
        else copy(entries = entries.toMutableList().also { it[index] = profile })

    /** Convenience for the preset editor, which only ever changes one duration. */
    fun withRestPresetAt(profileIndex: Int, presetIndex: Int, duration: Duration): Profiles {
        val profile = entries.getOrNull(profileIndex) ?: return this
        return replaceAt(
            profileIndex,
            profile.copy(restPresets = profile.restPresets.withDurationAt(presetIndex, duration)),
        )
    }

    companion object {
        const val COUNT = 3

        val DEFAULT = Profiles(
            listOf(
                WorkoutProfile(
                    kind = ExerciseKind.WEIGHTS,
                    restPresets = RestPresets.of(listOf(60.seconds, 90.seconds, 2.minutes)),
                    counterLabel = CounterLabel.SETS,
                ),
                WorkoutProfile(
                    kind = ExerciseKind.TREADMILL,
                    restPresets = RestPresets.of(listOf(30.seconds, 60.seconds, 2.minutes)),
                    counterLabel = CounterLabel.LAPS,
                ),
                WorkoutProfile(
                    kind = ExerciseKind.CYCLING,
                    restPresets = RestPresets.of(listOf(30.seconds, 60.seconds, 2.minutes)),
                    counterLabel = CounterLabel.ROUNDS,
                ),
            ),
        )

        /**
         * The only way persisted data should enter. Pads and trims to exactly
         * [COUNT] and clamps the selection, so stored data of the wrong shape
         * degrades instead of throwing.
         */
        fun of(entries: List<WorkoutProfile>, selected: Int): Profiles {
            val padded = List(COUNT) { index ->
                entries.getOrNull(index) ?: DEFAULT.entries[index]
            }
            return Profiles(padded, selected.coerceIn(0, COUNT - 1))
        }
    }
}
