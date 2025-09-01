package wtf.ndu.vibin.utils

import wtf.ndu.vibin.processing.ThumbnailProcessor
import java.awt.Color
import java.awt.Image
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.sqrt

object ImageUtils {

    data class ColorScheme(val primary: Color, val light: Color, val dark: Color)

    private const val MAX_COLOR_DIFFERENCE = 25

    fun getColorThemeFromImage(image: Image, maxDiff: Int = MAX_COLOR_DIFFERENCE): ColorScheme? {

        val resized = ThumbnailProcessor.scaleImage(image, 100, Image.SCALE_FAST)
        val resizedImage = ByteArrayInputStream(resized).use {
            ImageIO.read(it)
        }

        val colormap = mutableMapOf<Color, MutableList<Color>>()

        for (y in 0 until resizedImage.height) {
            for (x in 0 until resizedImage.width) {
                val pixel = resizedImage.getRGB(x, y)
                val color = Color(pixel, false)

                val closestColor = colormap.keys.minByOrNull { getDifference(it, color) }
                if (closestColor != null && getDifference(closestColor, color) < maxDiff) {
                    colormap[closestColor]?.add(color)
                } else {
                    colormap[color] = mutableListOf(color)
                }
            }
        }

        val sortedMap = colormap.toList().sortedBy { it.second.size }.map { getAverageColor(it.second) }.reversed()

        val light = sortedMap.firstOrNull { it.red > 200 && it.green > 200 && it.blue > 200 }
        val dark = sortedMap.firstOrNull { it.red < 55 && it.green < 55 && it.blue < 55 }
        val primary = sortedMap.firstOrNull { it != light && it != dark && getNonBwColorChannels(it) >= 1 }

        if (primary == null) return null

        return ColorScheme(
            primary = primary,
            light = light ?: Color(255, 255, 255),
            dark = dark ?: Color(0, 0, 0)
        )
    }

    fun getDifference(c1: Color, c2: Color): Int {
        val redDiff = c1.red - c2.red
        val greenDiff = c1.green - c2.green
        val blueDiff = c1.blue - c2.blue
        return sqrt((redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff).toDouble()).toInt()
    }

    fun getAverageColor(colors: List<Color>): Color {
        val avgRed = colors.map { it.red }.average().toInt()
        val avgGreen = colors.map { it.green }.average().toInt()
        val avgBlue = colors.map { it.blue }.average().toInt()
        return Color(avgRed, avgGreen, avgBlue)
    }

    fun getHexFromColor(color: Color): String {
        return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
    }

    fun getNonBwColorChannels(color: Color): Int {
        return listOf(color.red, color.green, color.blue).count { it in 110..200 }
    }
}
