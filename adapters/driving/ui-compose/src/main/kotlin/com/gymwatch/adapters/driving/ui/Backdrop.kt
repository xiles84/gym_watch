package com.gymwatch.adapters.driving.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource

/**
 * The skin's picture for [slot], dimmed until every label on it can be read.
 *
 * Draws nothing for a plain skin, so callers need not check. It is a layer, not
 * a modifier: call it first inside a full-screen `Box`, under the page, and the
 * page's own layout stays exactly as it was.
 */
@Composable
internal fun Backdrop(slot: WallpaperSlot) {
    val art = LocalSkinArt.current ?: return
    val palette = LocalPalette.current
    val res = art.forSlot(slot)
    val image = ImageBitmap.imageResource(res)
    val alpha = scrimFor(res, slot, image, palette)
    val rim = minOf(alpha, RIM_SCRIM)

    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                if (slot.textRadius >= 1f) {
                    drawRect(Color.Black.copy(alpha = alpha))
                } else {
                    drawRect(
                        Brush.radialGradient(
                            0f to Color.Black.copy(alpha = alpha),
                            slot.textRadius to Color.Black.copy(alpha = alpha),
                            1f to Color.Black.copy(alpha = rim),
                            center = center,
                            radius = size.minDimension / 2,
                        ),
                    )
                }
            },
    )
}

/**
 * Outside the text zone the art may be brighter — but only down to this, and
 * never brighter than the solved value. The rim still holds the pager dots and
 * the rest ring, which carry their own dark backing.
 */
private const val RIM_SCRIM = 0.3f

/**
 * Solved once per picture per process. A page that leaves the pager loses its
 * `remember`, and swiping back should not pay for ~180k pixels again. A picture
 * belongs to exactly one skin, so the resource id already implies the palette.
 * Only ever touched from composition, on the main thread.
 */
private val solvedScrims = HashMap<Pair<Int, WallpaperSlot>, Float>()

private fun scrimFor(res: Int, slot: WallpaperSlot, image: ImageBitmap, palette: GymPalette): Float =
    solvedScrims.getOrPut(res to slot) {
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        ScrimSolver.solve(pixels, image.width, image.height, slot.textRadius, palette.textColors)
    }
