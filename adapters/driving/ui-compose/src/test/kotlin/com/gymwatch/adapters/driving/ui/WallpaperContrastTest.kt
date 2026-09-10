package com.gymwatch.adapters.driving.ui

import com.gymwatch.core.domain.model.Contrast
import com.gymwatch.core.domain.model.Skin
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every wallpaper that ships, checked the way the watch will draw it.
 *
 * Runs [ScrimSolver] on the real JPEGs, then checks its answer independently:
 * composites the black overlay here, takes the exact 99th percentile by sorting
 * rather than by bins, and asserts every text role of the skin reaches AA over
 * it. A picture that needs more than [ScrimSolver.CAP] fails too — swap the
 * picture, do not raise the cap.
 */
class WallpaperContrastTest {

    private val resDir = File("src/main/res/drawable-nodpi")

    private val themed = Skin.entries.mapNotNull { skin -> artFor(skin)?.let { skin to it } }

    /** A drawable id back to its file, through the generated R class. */
    private fun fileFor(id: Int): File {
        val name = R.drawable::class.java.fields.single { it.getInt(null) == id }.name
        return File(resDir, "$name.jpg")
    }

    @Test
    fun `every wallpaper reads at AA under its solved scrim`() {
        val failures = mutableListOf<String>()

        themed.forEach { (skin, art) ->
            WallpaperSlot.entries.forEach { slot ->
                val file = fileFor(art.forSlot(slot))
                val image = ImageIO.read(file)
                val width = image.width
                val height = image.height
                val pixels = image.getRGB(0, 0, width, height, null, 0, width)

                val alpha = ScrimSolver.solve(pixels, width, height, slot.textRadius, skin.palette.textColors)

                val keep = 1f - alpha
                val zone = ScrimSolver.pixelsWithin(pixels, width, height, slot.textRadius)
                val dimmed = DoubleArray(zone.size) { i ->
                    val p = zone[i]
                    Contrast.luminance(
                        (((p shr 16) and 0xFF) * keep).roundToInt(),
                        (((p shr 8) and 0xFF) * keep).roundToInt(),
                        ((p and 0xFF) * keep).roundToInt(),
                    )
                }.sorted()
                val background = dimmed[ceil(dimmed.size * ScrimSolver.PERCENTILE).toInt() - 1]
                val worst = skin.palette.textColors.minOf { Contrast.ratio(Contrast.luminance(it), background) }

                val line = "%s %s (%s): scrim %.2f, worst text %.2f:1"
                    .format(skin.name, slot.name, file.name, alpha, worst)
                println(line)
                if (worst < Contrast.MIN_TEXT) failures += "$line — below AA"
                if (alpha > ScrimSolver.CAP) failures += "$line — needs more dimming than ${ScrimSolver.CAP}"
            }
        }

        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun `every shipped wallpaper belongs to a skin`() {
        // An orphan still ships in the APK, and usually means a renamed file.
        val used = themed
            .flatMap { (_, art) -> WallpaperSlot.entries.map { fileFor(art.forSlot(it)).name } }
            .toSet()
        val shipped = resDir.listFiles { file -> file.name.startsWith("wp_") }!!.map { it.name }.toSet()

        assertEquals(shipped, used)
    }

    @Test
    fun `the checks above have something to check`() {
        // A wrong working directory or an empty table would pass both with
        // nothing looked at.
        assertTrue("no wallpaper directory at ${resDir.absolutePath}", resDir.isDirectory)
        assertTrue("no skin has wallpapers", themed.isNotEmpty())
        themed.forEach { (skin, art) ->
            WallpaperSlot.entries.forEach { slot ->
                assertTrue("${skin.name} ${slot.name} has no file", fileFor(art.forSlot(slot)).isFile)
            }
        }
    }
}
