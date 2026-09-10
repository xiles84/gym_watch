package com.gymwatch.core.domain.model

/**
 * One skin's colours, as ARGB.
 *
 * `Long` rather than a Compose `Color` because the core never imports the
 * Android SDK — `ArchitectureTest` fails the build over it. Keeping the values
 * here rather than in the UI module means a skin is fully described by the
 * domain, so a second surface (a tile, a complication) renders the same skin
 * without a second copy of the palettes. `Color(palette.background)` takes a
 * `Long` directly, so the UI pays one conversion and nothing else.
 *
 * Eight roles, not ten: `Danger` and `StopBackground` went unused when the
 * Health Services screens were removed, and inventing a value for each of them
 * in every skin would be inventing work.
 */
data class Palette(
    val background: Long,
    val surface: Long,
    val onSurface: Long,
    val muted: Long,
    val dim: Long,
    val chrono: Long,
    val rest: Long,
    val go: Long,
)

/**
 * The colour schemes the watch can be set to.
 *
 * A fixed set rather than a colour picker, for the same reason the counter
 * label is a fixed set: choosing a hue on a 45mm screen with chalk on your
 * hands is not a thing anyone wants to do between sets.
 *
 * Every skin keeps a true black background. On an OLED watch black costs no
 * power and gives the numbers maximum contrast, which is the whole reason the
 * original palette looked the way it did — a skin changes the accents, not
 * that decision.
 */
enum class Skin(val title: String, val palette: Palette) {

    /** The palette the app shipped with, value for value. */
    ORIGINAL(
        title = "Original",
        palette = Palette(
            background = 0xFF000000L,
            surface = 0xFF2A2A2AL,
            onSurface = 0xFFE8E8E8L,
            muted = 0xFF7D7D7DL,
            dim = 0xFF555555L,
            chrono = 0xFF9FE1CBL,
            rest = 0xFFFAC775L,
            go = 0xFF97C459L,
        ),
    ),

    EMBER(
        title = "Ember",
        palette = Palette(
            background = 0xFF000000L,
            surface = 0xFF2B1E19L,
            onSurface = 0xFFF7EAE1L,
            muted = 0xFFA08375L,
            dim = 0xFF5C4037L,
            chrono = 0xFFFFB020L,
            rest = 0xFFFF6B4AL,
            go = 0xFFFFD166L,
        ),
    ),

    ULTRAVIOLET(
        title = "Ultraviolet",
        palette = Palette(
            background = 0xFF000000L,
            surface = 0xFF241C3AL,
            onSurface = 0xFFEDE7FFL,
            muted = 0xFF8F86B8L,
            dim = 0xFF4A4270L,
            chrono = 0xFF7CF5FFL,
            rest = 0xFFC77DFFL,
            go = 0xFF56E39FL,
        ),
    );

    companion object {
        val DEFAULT = ORIGINAL

        /**
         * The only way persisted data should enter. A name from a version that
         * had a skin this one does not falls back rather than throwing.
         */
        fun of(name: String?): Skin = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
