package com.gymwatch.adapters.driven.platform

import com.gymwatch.core.domain.model.ExerciseKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every [ExerciseKind] must name something Samsung Health actually has.
 *
 * A typo here fails silently on the watch — a blank circle, or a start screen
 * that never opens — so the names are checked against snapshots taken from
 * Samsung Health 7.00.0.131 with `aapt2 dump resources` and `dexdump`
 * (docs/LESSONS.md #28). If a Samsung Health update renames something, refresh
 * the snapshot the same way; do not loosen the test.
 */
class SamsungExercisesTest {

    @Test
    fun `every kind maps to an exercise type Samsung Health knows`() {
        val unknown = ExerciseKind.entries.filter { it.samsung().type !in SH_EXERCISE_TYPES }
        assertTrue("not in Samsung Health's ExerciseType: $unknown", unknown.isEmpty())
    }

    @Test
    fun `no kind maps to a type the start screen cannot open`() {
        val programmes = ExerciseKind.entries.filter { it.samsung().type in NOT_STARTABLE }
        assertTrue("mapped to a programme or placeholder: $programmes", programmes.isEmpty())
    }

    @Test
    fun `no two kinds open the same exercise`() {
        val types = ExerciseKind.entries.map { it.samsung().type }
        assertEquals(types.size, types.toSet().size)
    }

    @Test
    fun `every kind has an icon in Samsung Health`() {
        val missing = ExerciseKind.entries.filter { it.samsung().icon !in SH_ICONS }
        assertTrue("no such Samsung Health drawable: $missing", missing.isEmpty())
    }

    private companion object {
        /** `Exercise.ExerciseType` constants, in declaration order. */
        val SH_EXERCISE_TYPES = setOf(
            "OTHER_WORKOUT", "WALKING", "RUNNING", "FLOOR", "TRACK_RUNNING", "TRAIL_RUNNING",
            "PACE_RUNNING", "PERIODIZATION_RUNNING", "FITNESS_PROGRAM_RUNNING", "BASEBALL",
            "SOFTBALL", "CRICKET", "GOLF", "BOWLING", "HOCKEY", "RUGBY", "BASKETBALL", "FOOTBALL",
            "HANDBALL", "AMERICAN_FOOTBALL", "VOLLEYBALL", "BEACH_VOLLEYBALL", "SQUASH", "TENNIS",
            "BADMINTON", "TABLE_TENNIS", "RACQUETBALL", "BOXING", "MARTIAL_ARTS", "BALLET",
            "DANCING", "BALLROOM_DANCING", "ZUMBA", "PILATES", "YOGA", "STRETCHING",
            "ROPE_SKIPPING", "HULA_HOPING", "PUSH_UP", "PULL_UP", "SIT_UP", "CIRCUIT_TRAINING",
            "MOUNTAIN_CLIMBER", "JUMPING_JACK", "BURPEE_TEST", "BENCH_PRESS", "SQUAT", "LUNGE",
            "LEG_PRESS", "LEG_EXTENSION", "LEG_CURL", "BACK_EXTENSION", "LAT_PULLDOWN", "DEADLIFT",
            "SHOULDER_PRESS", "FRONT_RAISE", "LATERAL_RAISE", "CRUNCH", "LEG_RAISE", "PLANK",
            "ARM_CURL", "ARM_EXTENSION", "SKATER", "HIGH_KNEE", "INLINE_SKATING", "HANG_GLIDING",
            "ARCHERY", "HORSE_RIDING", "CYCLING", "FLYING_DISC", "ROLLER_SKATING", "AEROBICS",
            "HIKING", "ROCK_CLIMBING", "BACKPACKING", "MOUNTAIN_BIKING", "ORIENTEERING",
            "SWIMMING_INSIDE", "SWIMMING_OUTSIDE", "AQUAROBICS", "CANOEING", "SAILING",
            "SCUBA_DIVING", "SNORKELLING", "KAYAKING", "KITESURFING", "RAFTING", "ROWING",
            "WINDSURFING", "YACHTING", "WATER_SKIING", "STEP_MACHINE", "WEIGHT_MACHINE",
            "EXERCISE_BIKE", "ROWING_MACHINE", "TREADMILL", "ELLIPTICAL", "STAIR_MACHINE",
            "CROSS_COUNTRY_SKIING", "SKIING", "ICE_DANCING", "ICE_SKATING", "ICE_HOCKING",
            "SNOWBOARDING", "ALPINE_SKIING", "SNOWSHOEING", "UNDEFINED",
        )

        /** Programmes, auto-detected floors and the sentinel: real types, not workouts you start. */
        val NOT_STARTABLE = setOf(
            "FLOOR", "PACE_RUNNING", "PERIODIZATION_RUNNING", "FITNESS_PROGRAM_RUNNING", "UNDEFINED",
        )

        /** `drawable/b_exercise_*` names. Note the one without `_icon`. */
        val SH_ICONS = listOf(
            "aerobics", "alpine_skiing", "archery", "arm_curl_biceps", "arm_extension_triceps",
            "back_extension", "backpacking", "badminton", "ballet", "ballroom_dance", "baseball",
            "basketball", "bench_press", "bowling", "boxing", "burpee_test", "canoeing",
            "circuit_training", "cricket", "cross_country_skiing", "crunch", "cycling", "dancing",
            "deadlift", "elliptical_trainer", "exercise_bike", "field_hockey", "football_american",
            "frisbee", "front_raise", "golf", "handball", "hang_gliding", "high_knees", "hiking",
            "horseback_riging", "hula_hooping", "ice_dancing", "ice_hockey", "ice_skating",
            "inline_skating", "jump_rope", "kayaking", "kite_surfing", "lat_pull_down",
            "lateral_raise", "leg_curl", "leg_extension", "leg_press", "leg_raise", "lunge",
            "martial_arts", "mountain_biking", "mountain_climber", "open_water_swimming",
            "orienteering", "others_workout", "pilates", "plank", "pt_jump", "pull_up_chin_ups",
            "push_up_press_ups", "racquetball", "rafting", "rock_climbing", "roller_skating",
            "rowing", "rowing_machine", "rugby", "running", "sailing", "shoulder_press",
            "sit_up_sit_ups", "skater", "skiing", "skin_scuba_diving", "snorkelling",
            "snow_shoeing", "snowboarding", "soccer_football", "softball", "squash", "squat",
            "stair_machine", "stairs", "step_machine", "stretching", "swimming", "table_tennis",
            "tennis", "track_running", "trail_running", "treadmil", "voeyball_beach", "volleyball",
            "walking", "water_skiing", "weight_machine", "windsurfing", "yachting", "yoga", "zumba",
        ).map { "b_exercise_${it}_icon" }.toSet() + "b_exercise_aquarobics"
    }
}
