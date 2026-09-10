package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class WorkoutSetupTest {

    @Test
    fun `the default has three shortcuts`() {
        assertEquals(WorkoutSetup.SHORTCUT_COUNT, WorkoutSetup.DEFAULT.shortcuts.size)
    }

    @Test
    fun `withShortcutAt swaps one slot and keeps the others`() {
        val changed = WorkoutSetup.DEFAULT.withShortcutAt(1, ExerciseKind.BENCH_PRESS)

        assertEquals(ExerciseKind.BENCH_PRESS, changed.shortcuts[1])
        assertEquals(WorkoutSetup.DEFAULT.shortcuts[0], changed.shortcuts[0])
        assertEquals(WorkoutSetup.DEFAULT.shortcuts[2], changed.shortcuts[2])
    }

    @Test
    fun `withShortcutAt ignores an index outside the range`() {
        assertEquals(WorkoutSetup.DEFAULT, WorkoutSetup.DEFAULT.withShortcutAt(9, ExerciseKind.YOGA))
    }

    @Test
    fun `withRestPresetAt changes one duration and no shortcut`() {
        val changed = WorkoutSetup.DEFAULT.withRestPresetAt(2, 45.seconds)

        assertEquals(45.seconds, changed.restPresets[2])
        assertEquals(WorkoutSetup.DEFAULT.restPresets[0], changed.restPresets[0])
        assertEquals(WorkoutSetup.DEFAULT.shortcuts, changed.shortcuts)
    }

    @Test
    fun `of pads short stored data and trims long`() {
        val short = WorkoutSetup.of(listOf(ExerciseKind.ROWING_MACHINE), RestPresets.DEFAULT)
        assertEquals(WorkoutSetup.SHORTCUT_COUNT, short.shortcuts.size)
        assertEquals(ExerciseKind.ROWING_MACHINE, short.shortcuts[0])
        assertEquals(WorkoutSetup.DEFAULT.shortcuts[1], short.shortcuts[1])

        val long = WorkoutSetup.of(ExerciseKind.entries.take(5), RestPresets.DEFAULT)
        assertEquals(WorkoutSetup.SHORTCUT_COUNT, long.shortcuts.size)
    }

    @Test
    fun `shortcutAt falls back rather than throwing on a bad index`() {
        assertEquals(WorkoutSetup.DEFAULT.shortcuts[0], WorkoutSetup.DEFAULT.shortcutAt(42))
    }
}
