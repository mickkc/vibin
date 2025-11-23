package de.mickkc.vibin.widgets

object WidgetUtils {

    fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val r = ((color1 shr 16 and 0xff) * (1 - ratio) + (color2 shr 16 and 0xff) * ratio).toInt()
        val g = ((color1 shr 8 and 0xff) * (1 - ratio) + (color2 shr 8 and 0xff) * ratio).toInt()
        val b = ((color1 and 0xff) * (1 - ratio) + (color2 and 0xff) * ratio).toInt()
        return (0xff shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }
}