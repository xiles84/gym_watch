package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RestTimerTest {

    @Test
    fun `idle timer reports its full duration`() {
        val timer = RestTimer(duration = 90.seconds)
        assertEquals(90.seconds, timer.remainingAt(5.minutes))
        assertFalse(timer.isRunning)
    }

    @Test
    fun `remaining counts down from the start mark`() {
        val timer = RestTimer(duration = 90.seconds).start(now = 10.seconds)
        assertEquals(60.seconds, timer.remainingAt(40.seconds))
    }

    @Test
    fun `remaining clamps at zero and never goes negative`() {
        val timer = RestTimer(duration = 30.seconds).start(0.seconds)
        assertEquals(Duration.ZERO, timer.remainingAt(45.seconds))
        assertTrue(timer.hasExpiredAt(45.seconds))
    }

    @Test
    fun `an idle timer has not expired even at a large clock value`() {
        assertFalse(RestTimer(duration = 30.seconds).hasExpiredAt(9.minutes))
    }

    @Test
    fun `a countdown in progress asks before resetting`() {
        val timer = RestTimer(duration = 30.seconds).start(0.seconds)

        assertTrue(timer.needsResetConfirmationAt(10.seconds))
        assertFalse(timer.isAlarmingAt(10.seconds))
    }

    @Test
    fun `at zero it alarms and resets without asking`() {
        val timer = RestTimer(duration = 30.seconds).start(0.seconds)

        assertTrue(timer.isAlarmingAt(30.seconds))
        assertFalse(timer.needsResetConfirmationAt(30.seconds))
        // Still ringing long after: the alarm is derived from the clock, so a
        // screen that slept through zero wakes up to it.
        assertTrue(timer.isAlarmingAt(20.minutes))
    }

    @Test
    fun `an idle or cancelled timer neither alarms nor asks`() {
        val idle = RestTimer(duration = 30.seconds)
        val cancelled = idle.start(0.seconds).cancel()

        listOf(idle, cancelled).forEach {
            assertFalse(it.isAlarmingAt(5.minutes))
            assertFalse(it.needsResetConfirmationAt(5.minutes))
        }
    }

    @Test
    fun `progress runs from one down to zero`() {
        val timer = RestTimer(duration = 60.seconds).start(0.seconds)
        assertEquals(1f, timer.progressAt(0.seconds))
        assertEquals(0.5f, timer.progressAt(30.seconds))
        assertEquals(0f, timer.progressAt(60.seconds))
    }

    @Test
    fun `duration is clamped to the allowed range`() {
        assertEquals(RestTimer.MIN, RestTimer().withDuration(1.seconds).duration)
        assertEquals(RestTimer.MAX, RestTimer().withDuration(2.minutes * 60).duration)
    }

    @Test
    fun `changing duration cancels any run in progress`() {
        val changed = RestTimer(duration = 60.seconds).start(0.seconds).withDuration(75.seconds)
        assertEquals(75.seconds, changed.duration)
        assertFalse(changed.isRunning)
    }
}
