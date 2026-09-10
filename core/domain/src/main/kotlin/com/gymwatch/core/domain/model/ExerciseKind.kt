package com.gymwatch.core.domain.model

/**
 * The workouts a shortcut can open.
 *
 * Every predefined type the watch's health app can start, not a curated few: a
 * shortcut only chooses what to open, so there is no reason to refuse a workout
 * the health app itself offers.
 *
 * The names are generic on purpose. The mapping to Samsung Health's own
 * identifiers and icons lives in the platform adapter, so the domain does not
 * know which health app is on the wrist — and Samsung's spellings
 * (`ICE_HOCKING`, `SWIMMING_INSIDE`) stay out of it.
 *
 * Declaration order is the picker's order: gym first, because that is where
 * this app is used.
 */
enum class ExerciseKind(val displayName: String) {
    // Gym machines
    WEIGHT_MACHINE("Weight machine"),
    TREADMILL("Treadmill"),
    EXERCISE_BIKE("Exercise bike"),
    ELLIPTICAL("Elliptical"),
    ROWING_MACHINE("Rowing machine"),
    STEP_MACHINE("Step machine"),
    STAIR_MACHINE("Stair machine"),
    CIRCUIT_TRAINING("Circuit training"),

    // Strength
    BENCH_PRESS("Bench press"),
    SQUAT("Squat"),
    DEADLIFT("Deadlift"),
    LUNGE("Lunge"),
    LEG_PRESS("Leg press"),
    LEG_EXTENSION("Leg extension"),
    LEG_CURL("Leg curl"),
    BACK_EXTENSION("Back extension"),
    LAT_PULLDOWN("Lat pulldown"),
    SHOULDER_PRESS("Shoulder press"),
    FRONT_RAISE("Front raise"),
    LATERAL_RAISE("Lateral raise"),
    ARM_CURL("Arm curl"),
    ARM_EXTENSION("Arm extension"),
    CRUNCH("Crunch"),
    LEG_RAISE("Leg raise"),
    PLANK("Plank"),
    PUSH_UP("Push-up"),
    PULL_UP("Pull-up"),
    SIT_UP("Sit-up"),

    // Bodyweight and dance
    BURPEE_TEST("Burpee test"),
    MOUNTAIN_CLIMBER("Mountain climber"),
    JUMPING_JACK("Jumping jack"),
    HIGH_KNEE("High knees"),
    SKATER("Skaters"),
    ROPE_SKIPPING("Jump rope"),
    HULA_HOOPING("Hula hooping"),
    AEROBICS("Aerobics"),
    ZUMBA("Zumba"),
    DANCING("Dancing"),
    BALLET("Ballet"),
    BALLROOM_DANCING("Ballroom dancing"),

    // Mind and body
    YOGA("Yoga"),
    PILATES("Pilates"),
    STRETCHING("Stretching"),

    // Running, walking and outdoors
    RUNNING("Running"),
    WALKING("Walking"),
    TRACK_RUNNING("Track running"),
    TRAIL_RUNNING("Trail running"),
    CYCLING("Cycling"),
    MOUNTAIN_BIKING("Mountain biking"),
    HIKING("Hiking"),
    BACKPACKING("Backpacking"),
    ROCK_CLIMBING("Rock climbing"),
    ORIENTEERING("Orienteering"),
    INLINE_SKATING("Inline skating"),
    ROLLER_SKATING("Roller skating"),
    HANG_GLIDING("Hang gliding"),
    HORSE_RIDING("Horse riding"),
    ARCHERY("Archery"),

    // Combat
    BOXING("Boxing"),
    MARTIAL_ARTS("Martial arts"),

    // Sports
    FOOTBALL("Football"),
    AMERICAN_FOOTBALL("American football"),
    BASKETBALL("Basketball"),
    VOLLEYBALL("Volleyball"),
    BEACH_VOLLEYBALL("Beach volleyball"),
    HANDBALL("Handball"),
    RUGBY("Rugby"),
    HOCKEY("Hockey"),
    BASEBALL("Baseball"),
    SOFTBALL("Softball"),
    CRICKET("Cricket"),
    TENNIS("Tennis"),
    SQUASH("Squash"),
    BADMINTON("Badminton"),
    TABLE_TENNIS("Table tennis"),
    RACQUETBALL("Racquetball"),
    GOLF("Golf"),
    BOWLING("Bowling"),
    FLYING_DISC("Frisbee"),

    // Water
    POOL_SWIMMING("Pool swim"),
    OPEN_WATER_SWIMMING("Open water swim"),
    AQUAROBICS("Aquarobics"),
    ROWING("Rowing"),
    CANOEING("Canoeing"),
    KAYAKING("Kayaking"),
    RAFTING("Rafting"),
    SAILING("Sailing"),
    YACHTING("Yachting"),
    WINDSURFING("Windsurfing"),
    KITESURFING("Kitesurfing"),
    WATER_SKIING("Water skiing"),
    SCUBA_DIVING("Scuba diving"),
    SNORKELLING("Snorkelling"),

    // Winter
    SKIING("Skiing"),
    ALPINE_SKIING("Alpine skiing"),
    CROSS_COUNTRY_SKIING("Cross-country skiing"),
    SNOWBOARDING("Snowboarding"),
    SNOWSHOEING("Snowshoeing"),
    ICE_SKATING("Ice skating"),
    ICE_DANCING("Ice dancing"),
    ICE_HOCKEY("Ice hockey"),

    OTHER_WORKOUT("Other workout");

    companion object {
        /**
         * The only way persisted data should enter.
         *
         * Names from before the full list keep working: the three kinds that
         * were renamed map to their nearest type, and anything else unknown
         * decodes to null rather than throwing.
         */
        fun of(name: String?): ExerciseKind? =
            LEGACY[name] ?: entries.firstOrNull { it.name == name }

        private val LEGACY = mapOf(
            "WEIGHTS" to WEIGHT_MACHINE,
            "STAIR_CLIMBING" to STAIR_MACHINE,
            "HIIT" to CIRCUIT_TRAINING,
        )
    }
}
