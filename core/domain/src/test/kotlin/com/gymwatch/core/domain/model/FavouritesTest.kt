package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavouritesTest {

    @Test
    fun `default has three pinned activities`() {
        assertEquals(Favourites.MAX, Favourites.DEFAULT.kinds.size)
    }

    @Test
    fun `toggle removes an already-pinned activity`() {
        val without = Favourites.DEFAULT.toggle(ExerciseKind.WEIGHTS)
        assertTrue(ExerciseKind.WEIGHTS !in without.kinds)
    }

    @Test
    fun `toggle refuses to add beyond the cap`() {
        val full = Favourites.DEFAULT               // already at MAX
        assertEquals(full, full.toggle(ExerciseKind.ROWING))
    }

    @Test
    fun `toggle adds when there is room`() {
        val room = Favourites(listOf(ExerciseKind.WEIGHTS))
        assertEquals(2, room.toggle(ExerciseKind.YOGA).kinds.size)
    }

    @Test
    fun `of drops duplicates and trims to the cap`() {
        val messy = listOf(
            ExerciseKind.WEIGHTS, ExerciseKind.WEIGHTS, ExerciseKind.RUNNING,
            ExerciseKind.YOGA, ExerciseKind.ROWING,
        )
        assertEquals(Favourites.MAX, Favourites.of(messy).kinds.size)
    }
}
