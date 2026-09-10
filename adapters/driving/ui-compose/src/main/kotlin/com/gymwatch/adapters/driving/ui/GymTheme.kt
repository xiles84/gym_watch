package com.gymwatch.adapters.driving.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * A deliberately tiny palette. On an OLED watch a true black background costs
 * no power and gives the numbers maximum contrast, which is what matters when
 * you glance at it mid-set.
 */
internal object GymColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF2A2A2A)
    val OnSurface = Color(0xFFE8E8E8)
    val Muted = Color(0xFF7D7D7D)
    val Dim = Color(0xFF555555)

    val Chrono = Color(0xFF9FE1CB)
    val Rest = Color(0xFFFAC775)
    val Danger = Color(0xFFF09595)
    val Go = Color(0xFF97C459)
    val StopBackground = Color(0xFF4A1B1B)
}

@Composable
internal fun GymTheme(content: @Composable () -> Unit) = content()
