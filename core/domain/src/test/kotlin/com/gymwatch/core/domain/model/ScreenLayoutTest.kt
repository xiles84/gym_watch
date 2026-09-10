package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScreenLayoutTest {

    @Test
    fun `every screen is visible by default`() {
        assertEquals(AppScreen.entries.toList(), ScreenLayout.DEFAULT.visible)
    }

    @Test
    fun `toggling hides and shows a screen`() {
        val hidden = ScreenLayout.DEFAULT.toggle(AppScreen.COUNTER)
        assertFalse(hidden.isVisible(AppScreen.COUNTER))
        assertTrue(hidden.toggle(AppScreen.COUNTER).isVisible(AppScreen.COUNTER))
    }

    @Test
    fun `the last visible screen cannot be hidden`() {
        // Hiding your way down to nothing would leave a pager with no pages and
        // no route back into settings.
        val oneLeft = ScreenLayout(
            order = AppScreen.entries.toList(),
            hidden = AppScreen.entries.toSet() - AppScreen.REST_TIMER,
        )
        assertEquals(oneLeft, oneLeft.toggle(AppScreen.REST_TIMER))
        assertEquals(listOf(AppScreen.REST_TIMER), oneLeft.visible)
    }

    @Test
    fun `visible is never empty even if stored data says otherwise`() {
        val corrupt = ScreenLayout(AppScreen.entries.toList(), AppScreen.entries.toSet())
        assertEquals(1, corrupt.visible.size)
    }

    @Test
    fun `hiding preserves position so a screen returns where it was`() {
        val roundTrip = ScreenLayout.DEFAULT
            .toggle(AppScreen.REST_TIMER)
            .toggle(AppScreen.REST_TIMER)
        assertEquals(ScreenLayout.DEFAULT.order, roundTrip.order)
    }

    @Test
    fun `move shifts one screen and leaves the rest in order`() {
        val moved = ScreenLayout.DEFAULT.move(AppScreen.WORKOUTS, -1)
        assertEquals(
            listOf(
                AppScreen.CHRONOMETER,
                AppScreen.REST_TIMER,
                AppScreen.WORKOUTS,
                AppScreen.COUNTER,
            ),
            moved.order,
        )
    }

    @Test
    fun `move off either end is a no-op`() {
        assertEquals(ScreenLayout.DEFAULT, ScreenLayout.DEFAULT.move(AppScreen.CHRONOMETER, -1))
        assertEquals(ScreenLayout.DEFAULT, ScreenLayout.DEFAULT.move(AppScreen.WORKOUTS, 1))
    }

    @Test
    fun `of appends screens missing from stored data`() {
        // A screen added in a later version must not vanish because an older
        // install's stored order predates it.
        val partial = ScreenLayout.of(listOf(AppScreen.COUNTER), emptySet())
        assertEquals(AppScreen.entries.size, partial.order.size)
        assertEquals(AppScreen.COUNTER, partial.order.first())
    }

    @Test
    fun `the old PROFILES name decodes to the workouts screen`() {
        // Otherwise an upgraded watch would lose the screen's position and any
        // hidden state, and a request for it would land on the first page.
        assertEquals(AppScreen.WORKOUTS, AppScreen.of("PROFILES"))
        assertEquals(AppScreen.COUNTER, AppScreen.of("COUNTER"))
        assertNull(AppScreen.of("NOPE"))
        assertNull(AppScreen.of(null))
    }
}
