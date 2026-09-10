package com.gymwatch.adapters.driving.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.gymwatch.core.domain.model.Palette
import com.gymwatch.core.domain.model.Skin

/**
 * The palette in Compose terms.
 *
 * The domain holds ARGB longs because it cannot see `androidx`; this is the one
 * place they become `Color`, and it happens once per skin change rather than
 * once per draw.
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
)

/**
 * Static rather than dynamic: a skin changes when you tap a row in settings and
 * never again, so re-composing the whole tree on that rare change is cheaper
 * than tracking every reader.
 */
internal val LocalPalette = staticCompositionLocalOf { Skin.DEFAULT.palette.toCompose() }

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
}

@Composable
internal fun GymTheme(skin: Skin, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalPalette provides skin.palette.toCompose(), content = content)
