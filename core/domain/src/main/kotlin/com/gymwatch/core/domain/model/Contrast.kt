package com.gymwatch.core.domain.model

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2 contrast: whether one colour can be read on another.
 *
 * In the domain because legibility is part of what makes a palette right, so
 * `SkinTest` can reject an unreadable skin without a device. The UI's wallpaper
 * dimming is solved against these same numbers.
 */
object Contrast {

    /** AA for text under 18pt, which is every label on a watch, hints included. */
    const val MIN_TEXT = 4.5

    /** One 8-bit sRGB channel as linear light, 0..1. */
    fun linear(channel: Int): Double {
        val c = channel / 255.0
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun luminance(red: Int, green: Int, blue: Int): Double =
        0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)

    /** Relative luminance of an ARGB colour; alpha is ignored. */
    fun luminance(argb: Long): Double = luminance(
        ((argb shr 16) and 0xFF).toInt(),
        ((argb shr 8) and 0xFF).toInt(),
        (argb and 0xFF).toInt(),
    )

    /** From 1:1 for identical colours to 21:1 for black on white, in either order. */
    fun ratio(luminanceA: Double, luminanceB: Double): Double =
        (max(luminanceA, luminanceB) + 0.05) / (min(luminanceA, luminanceB) + 0.05)

    fun ratio(argbA: Long, argbB: Long): Double = ratio(luminance(argbA), luminance(argbB))
}
