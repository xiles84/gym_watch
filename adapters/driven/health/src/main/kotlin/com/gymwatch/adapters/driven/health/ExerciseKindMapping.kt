package com.gymwatch.adapters.driven.health

import androidx.health.services.client.data.ExerciseType
import com.gymwatch.core.domain.model.ExerciseKind

/**
 * The domain vocabulary translated into Health Services vocabulary.
 *
 * This mapping is the entire reason the domain has its own [ExerciseKind] enum:
 * it keeps androidx.health out of the core, and it is the one file to edit if
 * the health backend is ever swapped.
 *
 * Where Health Services offers both an outdoor and a machine variant, the
 * machine variant wins - this is a gym app, so "Cycling" means the spin bike
 * and "Rowing" means the erg. That also gives better metrics, because the
 * machine types do not expect GPS.
 */
internal fun ExerciseKind.toExerciseType(): ExerciseType = when (this) {
    ExerciseKind.WEIGHTS -> ExerciseType.WEIGHTLIFTING
    ExerciseKind.TREADMILL -> ExerciseType.RUNNING_TREADMILL
    ExerciseKind.RUNNING -> ExerciseType.RUNNING
    ExerciseKind.WALKING -> ExerciseType.WALKING
    ExerciseKind.CYCLING -> ExerciseType.BIKING_STATIONARY
    ExerciseKind.ELLIPTICAL -> ExerciseType.ELLIPTICAL
    ExerciseKind.ROWING -> ExerciseType.ROWING_MACHINE
    ExerciseKind.STAIR_CLIMBING -> ExerciseType.STAIR_CLIMBING_MACHINE
    ExerciseKind.HIIT -> ExerciseType.HIGH_INTENSITY_INTERVAL_TRAINING
    ExerciseKind.YOGA -> ExerciseType.YOGA
}

internal fun ExerciseType.toExerciseKind(): ExerciseKind? =
    ExerciseKind.entries.firstOrNull { it.toExerciseType() == this }
