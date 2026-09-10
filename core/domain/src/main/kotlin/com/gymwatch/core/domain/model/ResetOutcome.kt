package com.gymwatch.core.domain.model

/**
 * What asking to reset a clock did.
 *
 * A clock that is counting is a set in progress, so a stray tap must not wipe
 * it: the reset waits for confirmation. A clock that is paused, idle or already
 * ringing has nothing left to lose, so it resets at once.
 */
enum class ResetOutcome { DONE, NEEDS_CONFIRMATION }
