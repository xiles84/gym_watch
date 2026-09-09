package com.gymwatch.core.domain.model

/**
 * The exercise types this app can start.
 *
 * Deliberately our own enum rather than Health Services' `ExerciseType`: the
 * domain must not know that Health Services exists. The mapping lives in the
 * health adapter, so swapping the health backend touches one file.
 */
enum class ExerciseKind(val displayName: String, val glyph: String) {
    WEIGHTS("Weights", "🏋"),
    TREADMILL("Treadmill", "🏃"),
    RUNNING("Running", "👟"),
    WALKING("Walking", "🚶"),
    CYCLING("Cycling", "🚴"),
    ELLIPTICAL("Elliptical", "⛷"),
    ROWING("Rowing", "🚣"),
    STAIR_CLIMBING("Stairs", "🪜"),
    HIIT("HIIT", "⚡"),
    YOGA("Yoga", "🧘"),
}
