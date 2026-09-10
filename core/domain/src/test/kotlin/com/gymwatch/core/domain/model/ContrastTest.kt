package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ContrastTest {

    @Test
    fun `black on white is the maximum, in either order`() {
        assertEquals(21.0, Contrast.ratio(0xFF000000L, 0xFFFFFFFFL), 1e-9)
        assertEquals(21.0, Contrast.ratio(0xFFFFFFFFL, 0xFF000000L), 1e-9)
        assertEquals(1.0, Contrast.ratio(0xFF7D7D7DL, 0xFF7D7D7DL), 1e-9)
    }

    @Test
    fun `the classic borderline grey lands just under AA`() {
        // #777 on white is the textbook 4.48:1 — close enough to 4.5 that a
        // wrong gamma curve or a swapped channel weight would show.
        assertEquals(4.48, Contrast.ratio(0xFF777777L, 0xFFFFFFFFL), 0.01)
    }

    @Test
    fun `the sRGB curve is linear below its knee and exact at its ends`() {
        assertEquals(0.0, Contrast.linear(0), 1e-12)
        assertEquals(1.0, Contrast.linear(255), 1e-12)
        assertEquals(10 / 255.0 / 12.92, Contrast.linear(10), 1e-12)
    }

    @Test
    fun `alpha does not change luminance`() {
        assertEquals(Contrast.luminance(0xFFFAC775L), Contrast.luminance(0x00FAC775L), 1e-12)
    }
}
