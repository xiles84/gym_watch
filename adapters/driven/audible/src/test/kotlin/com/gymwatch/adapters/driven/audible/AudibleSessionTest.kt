package com.gymwatch.adapters.driven.audible

import org.junit.Assert.assertEquals
import org.junit.Test

class AudibleSessionTest {

    @Test
    fun `a playing book has moved on since its position was reported`() {
        assertEquals(70_000L, AudibleSession.positionNow(reported = 10_000, reportedAt = 1_000, speed = 1f, playing = true, now = 61_000))
    }

    @Test
    fun `playback speed scales the time since the report`() {
        assertEquals(100_000L, AudibleSession.positionNow(reported = 10_000, reportedAt = 1_000, speed = 1.5f, playing = true, now = 61_000))
    }

    @Test
    fun `a paused book is where it was reported`() {
        assertEquals(10_000L, AudibleSession.positionNow(reported = 10_000, reportedAt = 1_000, speed = 1f, playing = false, now = 61_000))
    }

    @Test
    fun `an unknown position stays unknown`() {
        assertEquals(-1L, AudibleSession.positionNow(reported = -1, reportedAt = 1_000, speed = 1f, playing = true, now = 61_000))
    }
}
