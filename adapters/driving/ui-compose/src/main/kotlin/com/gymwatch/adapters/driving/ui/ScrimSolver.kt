package com.gymwatch.adapters.driving.ui

import com.gymwatch.core.domain.model.Contrast
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * How much black to lay over a wallpaper so every label on it can be read.
 *
 * Solved from the picture rather than tuned by hand: a night sky and a pale
 * pink one need different amounts, and a fixed value would bury one or fail the
 * other. Plain Kotlin with no Android in it, so `WallpaperContrastTest` runs
 * exactly this over the shipped JPEGs.
 */
internal object ScrimSolver {

    /** Every page keeps at least this much, so the pictures read as one app's. */
    const val FLOOR = 0.2f

    /** Past this the art is mostly gone; the test fails and the picture should change. */
    const val CAP = 0.8f

    /** The 99th percentile, not the brightest pixel: one star should not dim a whole sky. */
    const val PERCENTILE = 0.99

    private const val BINS = 4096

    private val linear = DoubleArray(256) { Contrast.linear(it) }

    /**
     * The lowest alpha at which the dimmest of [textColors] reaches
     * [Contrast.MIN_TEXT] against the [PERCENTILE] pixel inside [radius] (a
     * fraction of the screen's radius), never below [FLOOR]. The pixels above
     * the percentile are what the text shadow is for.
     */
    fun solve(argb: IntArray, width: Int, height: Int, radius: Float, textColors: List<Long>): Float {
        val dimmest = textColors.minOf { Contrast.luminance(it) }
        val target = (dimmest + 0.05) / Contrast.MIN_TEXT - 0.05
        if (target <= 0.0) return 1f

        val zone = pixelsWithin(argb, width, height, radius)
        if (percentileLuminance(zone, 0f) <= target) return FLOOR

        var low = 0f
        var high = 1f
        repeat(12) {
            val mid = (low + high) / 2
            if (percentileLuminance(zone, mid) <= target) high = mid else low = mid
        }
        return maxOf(FLOOR, high)
    }

    /** The pixels in a centred disc of [radius], as a fraction of the half-size. */
    fun pixelsWithin(argb: IntArray, width: Int, height: Int, radius: Float): IntArray {
        val cx = width / 2f
        val cy = height / 2f
        val r = radius * minOf(width, height) / 2f
        val out = IntArray(argb.size)
        var count = 0
        for (y in 0 until height) {
            val dy = y + 0.5f - cy
            for (x in 0 until width) {
                val dx = x + 0.5f - cx
                if (dx * dx + dy * dy <= r * r) out[count++] = argb[y * width + x]
            }
        }
        return out.copyOf(count)
    }

    /**
     * The luminance [PERCENTILE] of [zone] stays at or under once dimmed by
     * [alpha]. Binned and read from the top of its bin, so it can only
     * over-report, never under.
     */
    fun percentileLuminance(zone: IntArray, alpha: Float): Double {
        val keep = 1f - alpha
        val counts = IntArray(BINS)
        for (pixel in zone) {
            counts[minOf(BINS - 1, (luminanceOf(pixel, keep) * BINS).toInt())]++
        }
        val needed = ceil(zone.size * PERCENTILE).toInt()
        var seen = 0
        for (bin in 0 until BINS) {
            seen += counts[bin]
            if (seen >= needed) return (bin + 1).toDouble() / BINS
        }
        return 1.0
    }

    /**
     * One pixel's luminance under a black overlay, blended as Compose draws it:
     * in sRGB space, each 8-bit channel scaled by [keep]. Blending in linear
     * light instead predicts far more dimming than the watch needs.
     */
    fun luminanceOf(argb: Int, keep: Float): Double =
        0.2126 * linear[(((argb shr 16) and 0xFF) * keep).roundToInt()] +
            0.7152 * linear[(((argb shr 8) and 0xFF) * keep).roundToInt()] +
            0.0722 * linear[((argb and 0xFF) * keep).roundToInt()]
}
