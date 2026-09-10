package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkinTest {

    @Test
    fun `an unknown or missing name falls back to the default`() {
        // A skin removed in a later version must not crash an install that has
        // its name stored.
        assertEquals(Skin.DEFAULT, Skin.of("JUJUTSU_KAISEN"))
        assertEquals(Skin.DEFAULT, Skin.of(null))
        assertEquals(Skin.DEFAULT, Skin.of(""))
    }

    @Test
    fun `a known name round-trips`() {
        Skin.entries.forEach { assertEquals(it, Skin.of(it.name)) }
    }

    @Test
    fun `Original is the palette the app shipped with`() {
        // Pinned so a tidy-up of the colour list cannot quietly restyle the app
        // for everyone who never picked a skin.
        assertEquals(
            Palette(
                background = 0xFF000000L,
                surface = 0xFF2A2A2AL,
                onSurface = 0xFFE8E8E8L,
                muted = 0xFF7D7D7DL,
                dim = 0xFF555555L,
                chrono = 0xFF9FE1CBL,
                rest = 0xFFFAC775L,
                go = 0xFF97C459L,
            ),
            Skin.ORIGINAL.palette,
        )
    }

    @Test
    fun `every skin is opaque and distinguishable in the picker`() {
        Skin.entries.forEach { skin ->
            val roles = with(skin.palette) {
                listOf(background, surface, onSurface, muted, dim, chrono, rest, go)
            }
            // A zero alpha channel is the easy typo, and it renders invisible
            // rather than wrong, which makes it hard to spot on the device.
            roles.forEach { argb ->
                assertEquals(0xFFL, argb ushr 24, "${skin.name} has a non-opaque colour")
            }
        }

        // The picker identifies a skin by its chrono and rest dots.
        val dots = Skin.entries.map { it.palette.chrono to it.palette.rest }
        assertEquals(dots.size, dots.distinct().size, "two skins look the same in the picker")

        assertEquals(Skin.entries.size, Skin.entries.map { it.title }.distinct().size)
        assertTrue(Skin.entries.all { it.title.isNotBlank() })
    }
}
