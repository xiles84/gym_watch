package com.gymwatch.adapters.driven.platform

import com.gymwatch.core.domain.model.ExerciseKind

/** Samsung Health on the *watch*, not the phone package. */
internal const val SAMSUNG_HEALTH_PACKAGE = "com.samsung.android.wear.shealth"

/**
 * How Samsung Health refers to an [ExerciseKind].
 *
 * [type] is the constant of Samsung Health's internal `Exercise.ExerciseType`
 * enum that its start screen accepts in the `exercise.type` extra. [icon] is the
 * drawable it draws for that exercise. Both were read out of Samsung Health
 * 7.00.0.131 (docs/LESSONS.md #28). They are identifiers, not assets: the icon
 * itself is loaded from Samsung Health at runtime.
 */
internal data class SamsungExercise(val type: String, val icon: String)

/**
 * Most kinds share Samsung's name, so only the differences are listed. The
 * overrides are Samsung's own spellings, typos included (`ICE_HOCKING`,
 * `treadmil`, `horseback_riging`): they must match exactly, and
 * `SamsungExercisesTest` checks every kind against the real lists.
 */
internal fun ExerciseKind.samsung(): SamsungExercise = SamsungExercise(
    type = TYPE_OVERRIDES[this] ?: name,
    icon = ICON_OVERRIDES[this] ?: "b_exercise_${name.lowercase()}_icon",
)

private val TYPE_OVERRIDES = mapOf(
    ExerciseKind.POOL_SWIMMING to "SWIMMING_INSIDE",
    ExerciseKind.OPEN_WATER_SWIMMING to "SWIMMING_OUTSIDE",
    ExerciseKind.HULA_HOOPING to "HULA_HOPING",
    ExerciseKind.ICE_HOCKEY to "ICE_HOCKING",
)

private val ICON_OVERRIDES = mapOf(
    ExerciseKind.TREADMILL to "b_exercise_treadmil_icon",
    ExerciseKind.ELLIPTICAL to "b_exercise_elliptical_trainer_icon",
    ExerciseKind.LAT_PULLDOWN to "b_exercise_lat_pull_down_icon",
    ExerciseKind.ARM_CURL to "b_exercise_arm_curl_biceps_icon",
    ExerciseKind.ARM_EXTENSION to "b_exercise_arm_extension_triceps_icon",
    ExerciseKind.PUSH_UP to "b_exercise_push_up_press_ups_icon",
    ExerciseKind.PULL_UP to "b_exercise_pull_up_chin_ups_icon",
    ExerciseKind.SIT_UP to "b_exercise_sit_up_sit_ups_icon",
    ExerciseKind.JUMPING_JACK to "b_exercise_pt_jump_icon",
    ExerciseKind.HIGH_KNEE to "b_exercise_high_knees_icon",
    ExerciseKind.ROPE_SKIPPING to "b_exercise_jump_rope_icon",
    ExerciseKind.BALLROOM_DANCING to "b_exercise_ballroom_dance_icon",
    ExerciseKind.HORSE_RIDING to "b_exercise_horseback_riging_icon",
    ExerciseKind.FOOTBALL to "b_exercise_soccer_football_icon",
    ExerciseKind.AMERICAN_FOOTBALL to "b_exercise_football_american_icon",
    ExerciseKind.BEACH_VOLLEYBALL to "b_exercise_voeyball_beach_icon",
    ExerciseKind.HOCKEY to "b_exercise_field_hockey_icon",
    ExerciseKind.FLYING_DISC to "b_exercise_frisbee_icon",
    ExerciseKind.POOL_SWIMMING to "b_exercise_swimming_icon",
    ExerciseKind.AQUAROBICS to "b_exercise_aquarobics",
    ExerciseKind.KITESURFING to "b_exercise_kite_surfing_icon",
    ExerciseKind.SCUBA_DIVING to "b_exercise_skin_scuba_diving_icon",
    ExerciseKind.SNOWSHOEING to "b_exercise_snow_shoeing_icon",
    ExerciseKind.OTHER_WORKOUT to "b_exercise_others_workout_icon",
)
