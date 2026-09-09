package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CounterTest {

    @Test
    fun `increment and decrement move by one`() {
        assertEquals(1, Counter().increment().value)
        assertEquals(0, Counter(value = 1).decrement().value)
    }

    @Test
    fun `decrement floors at zero rather than going negative`() {
        assertEquals(0, Counter(value = 0).decrement().value)
    }

    @Test
    fun `increment caps at the maximum`() {
        assertEquals(Counter.MAX, Counter(value = Counter.MAX).increment().value)
    }

    @Test
    fun `stepBy clamps to the valid range in both directions`() {
        assertEquals(0, Counter(value = 3).stepBy(-10).value)
        assertEquals(Counter.MAX, Counter(value = 0).stepBy(5_000).value)
    }

    @Test
    fun `reset zeroes the value but keeps the label`() {
        val reset = Counter(value = 12, label = "BENCH").reset()
        assertEquals(0, reset.value)
        assertEquals("BENCH", reset.label)
    }

    @Test
    fun `labels are trimmed, truncated and never blank`() {
        assertEquals("BENCH", Counter().withLabel("  BENCH  ").label)
        assertEquals(Counter.DEFAULT_LABEL, Counter().withLabel("   ").label)
        assertEquals(Counter.MAX_LABEL_LENGTH, Counter().withLabel("x".repeat(50)).label.length)
    }
}
