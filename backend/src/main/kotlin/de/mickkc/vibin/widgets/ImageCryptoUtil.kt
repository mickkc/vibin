package de.mickkc.vibin.widgets

import de.mickkc.vibin.auth.CryptoUtil.secureRandom
import de.mickkc.vibin.utils.DateTimeUtils
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object ImageCryptoUtil {

    private val IMAGE_SIGNING_KEY: ByteArray = ByteArray(32).also { secureRandom.nextBytes(it) }

    private const val IMAGE_URL_VALIDITY_SECONDS = 3600L // 1 hour

    /**
     * Generates a signed URL for an image checksum with time-based expiration.
     *
     * @param checksum The image checksum (or "default-{type}" for default images)
     * @param quality The desired image quality/size
     * @return A URL path with signature and expiration parameters
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generateSignedImageUrl(checksum: String, quality: Int): String {

        val expirationTimestamp = DateTimeUtils.now() + IMAGE_URL_VALIDITY_SECONDS

        val hmacBytes = getHmacBytes(checksum, expirationTimestamp)

        val signature = Base64.UrlSafe.encode(hmacBytes).trimEnd('=')

        return "/api/widgets/images/$checksum?quality=$quality&exp=$expirationTimestamp&sig=$signature"
    }

    /**
     * Validates a signed image URL.
     * @param checksum The image checksum from the URL
     * @param expirationTimestamp The expiration timestamp from the URL (Unix seconds)
     * @param providedSignature The signature from the URL
     * @return true if URL is valid and not expired, false otherwise
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun validateImageSignature(checksum: String, expirationTimestamp: Long, providedSignature: String): Boolean {

        val currentTimestamp = DateTimeUtils.now()
        if (currentTimestamp > expirationTimestamp) {
            return false
        }

        val hmacBytes = getHmacBytes(checksum, expirationTimestamp)
        val expectedSignature = Base64.UrlSafe.encode(hmacBytes).trimEnd('=')

        return providedSignature == expectedSignature
    }

    private fun getHmacBytes(checksum: String, expirationTimestamp: Long): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(IMAGE_SIGNING_KEY, "HmacSHA256")
        mac.init(secretKeySpec)
        return mac.doFinal("$checksum:$expirationTimestamp".toByteArray())
    }
}