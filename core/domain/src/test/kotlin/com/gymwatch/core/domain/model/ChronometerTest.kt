package com.gymwatch.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChronometerTest {

    @Test
    fun `idle chronometer reads zero regardless of clock`() {
        assertEquals(Duration.ZERO, Chronometer.Idle.elapsedAt(5.minutes))
        assertFalse(Chronometer.Idle.isRunning)
    }

    @Test
    fun `elapsed is derived from the clock, not from ticks`() {
        val started = Chronometer.Idle.start(now = 10.seconds)
        // Nothing ran in between. The value is still correct — this is the
        // property that makes it survive screen-off.
        assertEquals(50.seconds, started.elapsedAt(60.seconds))
    }

    @Test
    fun `pause banks elapsed time and freezes the reading`() {
        val paused = Chronometer.Idle.start(10.seconds).pause(40.seconds)

        assertFalse(paused.isRunning)
        assertEquals(30.seconds, paused.elapsedAt(40.seconds))
        // Clock keeps moving; a paused chronometer must not.
        assertEquals(30.seconds, paused.elapsedAt(9.minutes))
    }

    @Test
    fun `resume accumulates across runs`() {
        val chrono = Chronometer.Idle
            .start(0.seconds)
            .pause(30.seconds)     // banked 30s
            .start(100.seconds)    // resumed after a 70s gap

        assertEquals(40.seconds, chrono.elapsedAt(110.seconds))
    }

    @Test
    fun `a long screen-off gap is counted correctly`() {
        val chrono = Chronometer.Idle.start(0.seconds)
        assertEquals(45.minutes, chrono.elapsedAt(45.minutes))
    }

    @Test
    fun `starting a running chronometer is a no-op`() {
        val running = Chronometer.Idle.start(10.seconds)
        assertEquals(running, running.start(90.seconds))
    }

    @Test
    fun `pausing a paused chronometer is a no-op`() {
        val paused = Chronometer.Idle.start(0.seconds).pause(10.seconds)
        assertEquals(paused, paused.pause(99.seconds))
    }

    @Test
    fun `laps record total elapsed and current lap resets`() {
        val chrono = Chronometer.Idle
            .start(0.seconds)
            .lap(30.seconds)
            .lap(50.seconds)

        assertEquals(listOf(30.seconds, 50.seconds), chrono.laps)
        assertEquals(20.seconds, chrono.currentLapAt(70.seconds))
    }

    @Test
    fun `lap is ignored while not running`() {
        assertTrue(Chronometer.Idle.lap(10.seconds).laps.isEmpty())
    }

    @Test
    fun `reset clears everything`() {
        val chrono = Chronometer.Idle.start(0.seconds).lap(10.seconds).reset()
        assertTrue(chrono.isReset)
    }

    @Test
    fun `only a running chronometer asks before resetting`() {
        val running = Chronometer.Idle.start(0.seconds)

        assertTrue(running.needsResetConfirmation)
        assertFalse(running.pause(10.seconds).needsResetConfirmation, "paused on purpose")
        assertFalse(Chronometer.Idle.needsResetConfirmation)
    }

    @Test
    fun `toggle flips between running and paused`() {
        val running = Chronometer.Idle.toggle(0.seconds)
        assertTrue(running.isRunning)
        assertFalse(running.toggle(10.seconds).isRunning)
    }
}
