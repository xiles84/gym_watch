package com.gymwatch.adapters.driving.ui

import androidx.annotation.DrawableRes
import com.gymwatch.core.domain.model.Skin

/**
 * Every surface that can carry a wallpaper. The confirm dialog is not one:
 * Wear's `AlertDialog` paints its own opaque background over the page.
 *
 * [textRadius] is how far from the centre, as a fraction of the screen's
 * radius, the surface draws text straight onto the picture. The scrim holds
 * its full strength out to there and eases off beyond it, so the art is
 * brightest where nothing has to be read. 1 means text reaches the rim and the
 * scrim is uniform.
 */
internal enum class WallpaperSlot(val textRadius: Float) {
    CHRONO(0.62f),
    REST(0.62f),
    REST_RUNNING(0.62f),
    REST_OVER(0.62f),
    COUNTER(0.62f),

    // The outer shortcuts' names reach most of the way to the rim.
    WORKOUTS(1f),
    SETTINGS(1f),
    REST_EDITOR(1f),
    WORKOUT_PICKER(1f),
}

/**
 * One themed skin's pictures.
 *
 * Six surfaces must have their own. The other three borrow their parent's until
 * a picture is drawn for them, which is why they are nullable: adding one later
 * is a resource from `scripts/wallpapers.ps1` and one argument here.
 */
internal class SkinArt(
    private val chrono: Int,
    private val rest: Int,
    private val restRunning: Int,
    private val counter: Int,
    private val workouts: Int,
    private val settings: Int,
    private val restOver: Int? = null,
    private val restEditor: Int? = null,
    private val workoutPicker: Int? = null,
) {
    @DrawableRes
    fun forSlot(slot: WallpaperSlot): Int = when (slot) {
        WallpaperSlot.CHRONO -> chrono
        WallpaperSlot.REST -> rest
        WallpaperSlot.REST_RUNNING -> restRunning
        WallpaperSlot.REST_OVER -> restOver ?: restRunning
        WallpaperSlot.COUNTER -> counter
        WallpaperSlot.WORKOUTS -> workouts
        WallpaperSlot.SETTINGS -> settings
        WallpaperSlot.REST_EDITOR -> restEditor ?: rest
        WallpaperSlot.WORKOUT_PICKER -> workoutPicker ?: workouts
    }
}

/**
 * Which pictures a skin draws behind its screens, or `null` for a plain one.
 *
 * Exhaustive on purpose: a new skin does not compile until someone decides
 * whether it has pictures, and a missing required picture is a compile error
 * rather than a blank page. The file name in `skin-images/<theme>/` is the
 * assignment; the script turns `rest-running.png` into `wp_<theme>_rest_running`.
 */
internal fun artFor(skin: Skin): SkinArt? = when (skin) {
    Skin.ORIGINAL, Skin.EMBER, Skin.ULTRAVIOLET -> null

    Skin.DRAGON_BALL -> SkinArt(
        chrono = R.drawable.wp_dragon_ball_chrono,
        rest = R.drawable.wp_dragon_ball_rest,
        restRunning = R.drawable.wp_dragon_ball_rest_running,
        counter = R.drawable.wp_dragon_ball_counter,
        workouts = R.drawable.wp_dragon_ball_workouts,
        settings = R.drawable.wp_dragon_ball_settings,
    )

    Skin.SAILOR_MOON -> SkinArt(
        chrono = R.drawable.wp_sailor_moon_chrono,
        rest = R.drawable.wp_sailor_moon_rest,
        restRunning = R.drawable.wp_sailor_moon_rest_running,
        counter = R.drawable.wp_sailor_moon_counter,
        workouts = R.drawable.wp_sailor_moon_workouts,
        settings = R.drawable.wp_sailor_moon_settings,
    )

    Skin.SPY_FAMILY -> SkinArt(
        chrono = R.drawable.wp_spy_family_chrono,
        rest = R.drawable.wp_spy_family_rest,
        restRunning = R.drawable.wp_spy_family_rest_running,
        counter = R.drawable.wp_spy_family_counter,
        workouts = R.drawable.wp_spy_family_workouts,
        settings = R.drawable.wp_spy_family_settings,
    )
}
