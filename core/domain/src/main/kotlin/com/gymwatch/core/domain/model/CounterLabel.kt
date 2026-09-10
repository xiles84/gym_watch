package com.gymwatch.core.domain.model

/**
 * What the counter is counting, chosen per profile.
 *
 * A fixed set rather than free text on purpose: entering text on a watch means
 * a keyboard or voice, and neither works with chalk on your hands.
 */
enum class CounterLabel { SETS, REPS, ROUNDS, LAPS }
