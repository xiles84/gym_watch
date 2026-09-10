package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExerciseKindTest {

    @Test
    fun `every current name decodes to itself`() {
        ExerciseKind.entries.forEach { assertEquals(it, ExerciseKind.of(it.name)) }
    }

    @Test
    fun `names renamed from the old short list still decode`() {
        // A watch upgraded from the profiles version has these stored.
        assertEquals(ExerciseKind.WEIGHT_MACHINE, ExerciseKind.of("WEIGHTS"))
        assertEquals(ExerciseKind.STAIR_MACHINE, ExerciseKind.of("STAIR_CLIMBING"))
        assertEquals(ExerciseKind.CIRCUIT_TRAINING, ExerciseKind.of("HIIT"))
    }

    @Test
    fun `old names that were kept decode unchanged`() {
        listOf("TREADMILL", "RUNNING", "WALKING", "CYCLING", "ELLIPTICAL", "ROWING", "YOGA")
            .forEach { assertEquals(it, ExerciseKind.of(it)?.name) }
    }

    @Test
    fun `unknown or missing names decode to null`() {
        assertNull(ExerciseKind.of("QUIDDITCH"))
        assertNull(ExerciseKind.of(null))
    }
}
