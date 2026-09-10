package com.gymwatch.adapters.driving.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.wear.compose.material3.LocalTextStyle
import com.gymwatch.core.domain.model.Palette
import com.gymwatch.core.domain.model.Skin

/**
 * The palette in Compose terms.
 *
 * The domain holds ARGB longs because it cannot see `androidx`; this is the one
 * place they become `Color`, and it happens once per skin change rather than
 * once per draw. [textColors] stays ARGB: it only ever feeds [ScrimSolver],
 * which is plain Kotlin.
 */
internal data class GymPalette(
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val muted: Color,
    val dim: Color,
    val chrono: Color,
    val rest: Color,
    val go: Color,
    val textColors: List<Long>,
)

private fun Palette.toCompose() = GymPalette(
    background = Color(background),
    surface = Color(surface),
    onSurface = Color(onSurface),
    muted = Color(muted),
    dim = Color(dim),
    chrono = Color(chrono),
    rest = Color(rest),
    go = Color(go),
    textColors = textColors,
)

/**
 * Static rather than dynamic: a skin changes when you tap a row in settings and
 * never again, so re-composing the whole tree on that rare change is cheaper
 * than tracking every reader.
 */
internal val LocalPalette = staticCompositionLocalOf { Skin.DEFAULT.palette.toCompose() }

/** The current skin's pictures, or `null` for a plain skin. Static for the same reason. */
internal val LocalSkinArt = staticCompositionLocalOf<SkinArt?> { null }

/**
 * Reads the current skin's colours.
 *
 * These stayed properties on an object, with `@Composable` getters, so that the
 * ~60 existing `GymColors.Rest` call sites did not have to change — the same
 * shape `MaterialTheme.colorScheme` uses. The catch is that a `@Composable`
 * getter cannot be read from a non-composable lambda: inside a `Canvas` block,
 * hoist [LocalPalette] into a local first.
 */
internal object GymColors {
    val Background: Color @Composable get() = LocalPalette.current.background
    val Surface: Color @Composable get() = LocalPalette.current.surface
    val OnSurface: Color @Composable get() = LocalPalette.current.onSurface
    val Muted: Color @Composable get() = LocalPalette.current.muted
    val Dim: Color @Composable get() = LocalPalette.current.dim
    val Chrono: Color @Composable get() = LocalPalette.current.chrono
    val Rest: Color @Composable get() = LocalPalette.current.rest
    val Go: Color @Composable get() = LocalPalette.current.go

    /**
     * A hairline round a tap target, so a dark button keeps its edge against
     * dimmed art. Transparent on a plain skin, where the black around a button
     * already outlines it.
     */
    val Outline: Color @Composable get() =
        if (LocalSkinArt.current == null) Color.Transparent else LocalPalette.current.onSurface.copy(alpha = 0.45f)

    /**
     * What a `Picker` fades its outer options into. The screen's black on a
     * plain skin; nothing over a wallpaper, where a black fade drew two dark
     * bars straight across the picture.
     */
    val PickerFade: Color @Composable get() =
        if (LocalSkinArt.current == null) LocalPalette.current.background else Color.Transparent
}

/**
 * Over a wallpaper, every piece of text also gets a soft black shadow. The
 * scrim is solved against the 99th-percentile pixel, so the brightest one
 * percent — a star, the shine on a dragon ball — can still sit under a letter;
 * the shadow keeps that letter's edge. Provided through [LocalTextStyle] so
 * none of the `Text` calls change.
 */
@Composable
internal fun GymTheme(skin: Skin, content: @Composable () -> Unit) {
    val art = artFor(skin)
    val textStyle = LocalTextStyle.current
    CompositionLocalProvider(
        LocalPalette provides skin.palette.toCompose(),
        LocalSkinArt provides art,
        LocalTextStyle provides if (art == null) textStyle else textStyle.copy(shadow = ArtTextShadow),
        content = content,
    )
}

private val ArtTextShadow = Shadow(color = Color.Black.copy(alpha = 0.8f), blurRadius = 8f)
