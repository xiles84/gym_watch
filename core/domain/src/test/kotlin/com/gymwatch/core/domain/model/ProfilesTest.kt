package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ProfilesTest {

    @Test
    fun `the default has three profiles with the first selected`() {
        assertEquals(Profiles.COUNT, Profiles.DEFAULT.entries.size)
        assertEquals(ExerciseKind.WEIGHTS, Profiles.DEFAULT.current.kind)
    }

    @Test
    fun `select changes the active profile`() {
        assertEquals(ExerciseKind.CYCLING, Profiles.DEFAULT.select(2).current.kind)
    }

    @Test
    fun `select ignores an index outside the range`() {
        assertEquals(Profiles.DEFAULT, Profiles.DEFAULT.select(9))
    }

    @Test
    fun `replaceAt swaps one slot and keeps the selection`() {
        val selected = Profiles.DEFAULT.select(2)
        val replaced = selected.replaceAt(0, WorkoutProfile(ExerciseKind.YOGA))

        assertEquals(ExerciseKind.YOGA, replaced.entries[0].kind)
        assertEquals(2, replaced.selected, "editing a slot must not move the selection")
        assertEquals(Profiles.DEFAULT.entries[1], replaced.entries[1])
    }

    @Test
    fun `withRestPresetAt changes one duration on one profile`() {
        val changed = Profiles.DEFAULT.withRestPresetAt(1, 2, 45.seconds)

        assertEquals(45.seconds, changed.entries[1].restPresets[2])
        assertEquals(Profiles.DEFAULT.entries[0].restPresets, changed.entries[0].restPresets)
    }

    @Test
    fun `withRestPresetAt on a missing profile is a no-op`() {
        assertEquals(Profiles.DEFAULT, Profiles.DEFAULT.withRestPresetAt(9, 0, 45.seconds))
    }

    @Test
    fun `of pads short stored data and clamps the selection`() {
        val partial = Profiles.of(listOf(WorkoutProfile(ExerciseKind.ROWING)), selected = 7)

        assertEquals(Profiles.COUNT, partial.entries.size)
        assertEquals(ExerciseKind.ROWING, partial.entries[0].kind)
        assertEquals(Profiles.COUNT - 1, partial.selected)
    }

    @Test
    fun `current falls back rather than throwing on a bad selection`() {
        val corrupt = Profiles(Profiles.DEFAULT.entries, selected = 42)
        assertEquals(Profiles.DEFAULT.entries[0], corrupt.current)
    }
}
