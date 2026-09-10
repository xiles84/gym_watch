package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class RestPresetsTest {

    @Test
    fun `presets are clamped to the timer's own bounds`() {
        val presets = RestPresets.of(listOf(1.seconds, 90.seconds, 3.hours))
        assertEquals(RestTimer.MIN, presets[0])
        assertEquals(90.seconds, presets[1])
        assertEquals(RestTimer.MAX, presets[2])
    }

    @Test
    fun `of pads a short stored list rather than failing`() {
        assertEquals(RestPresets.COUNT, RestPresets.of(listOf(30.seconds)).durations.size)
    }

    @Test
    fun `of trims a stored list that is too long`() {
        val tooMany = List(10) { 30.seconds }
        assertEquals(RestPresets.COUNT, RestPresets.of(tooMany).durations.size)
    }

    @Test
    fun `an out of range index falls back instead of throwing`() {
        // Reached only from corrupt storage, but a crash mid-set is not an option.
        assertEquals(RestPresets.DEFAULT.durations[0], RestPresets.DEFAULT[99])
    }

    @Test
    fun `changing one preset leaves the others alone`() {
        val changed = RestPresets.DEFAULT.withDurationAt(1, 45.seconds)
        assertEquals(45.seconds, changed[1])
        assertEquals(RestPresets.DEFAULT[0], changed[0])
        assertEquals(RestPresets.DEFAULT[2], changed[2])
    }

    @Test
    fun `changing an out of range preset is a no-op`() {
        assertEquals(RestPresets.DEFAULT, RestPresets.DEFAULT.withDurationAt(7, 45.seconds))
    }
}
