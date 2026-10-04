package de.mickkc.vibin.widgets

import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.utils.AccessCryptoUtil
import java.net.URI

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

    private val hexRegex = Regex("^[0-9a-fA-F]{6}$")

    fun colorFromHex(hex: String): Int? {
        if (!hexRegex.matches(hex)) return null
        return hex.toLong(16).toInt() or (0xFF shl 24)
    }

    /**
     * Generates a signed image URL from an ImageEntity.
     *
     * @param image The ImageEntity (nullable)
     * @param type The type of image ("track", "album", "user", etc.)
     * @param quality The desired quality/size of the image
     * @param baseUrl Prepended to the URL if set, see [WidgetContext.baseUrl]
     * @return A signed URL, or null if image is null
     */
    fun getImageUrl(image: ImageEntity?, type: String, quality: Int = 192, baseUrl: String?): String {

        return baseUrl.orEmpty() + AccessCryptoUtil.generateSignedImageUrl(
            checksum = image?.sourceChecksum ?: "default-$type",
            quality = quality
        )
    }

    /**
     * Validates a base URL sent by the embed script.
     *
     * @param url The URL the embed script was loaded from
     * @return The URL without a trailing slash, or null if it isn't a plain http(s) URL
     */
    fun parseBaseUrl(url: String): String? {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return null
        }

        if (uri.scheme?.lowercase() !in listOf("http", "https") || uri.host.isNullOrEmpty()) return null
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null

        return url.trimEnd('/')
    }
}
