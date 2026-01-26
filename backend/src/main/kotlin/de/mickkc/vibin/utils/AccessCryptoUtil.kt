package de.mickkc.vibin.utils

import de.mickkc.vibin.auth.CryptoUtil
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object AccessCryptoUtil {

    private val IMAGE_SIGNING_KEY: ByteArray = ByteArray(32).also { CryptoUtil.secureRandom.nextBytes(it) }

    private const val ACCESS_VALIDITY_SECONDS = 3600L // 1 hour

    /**
     * Generates a signed URL for an image checksum with time-based expiration.
     *
     * @param checksum The image checksum (or "default-{type}" for default images)
     * @param quality The desired image quality/size
     * @return A URL path with signature and expiration parameters
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generateSignedImageUrl(checksum: String, quality: Int): String {
        val expirationTimestamp = DateTimeUtils.now() + ACCESS_VALIDITY_SECONDS
        val hmacBytes = getHmacBytes(checksum, expirationTimestamp)
        val signature = Base64.UrlSafe.encode(hmacBytes).trimEnd('=')
        return "/api/widgets/images/$checksum?quality=$quality&exp=$expirationTimestamp&sig=$signature"
    }

    /**
     * Generates a signature and expiration timestamp for any given image checksum.
     *
     * @param checksum The checksum
     * @return A pair of (signature, expirationTimestamp)
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generateSignatureAndTimestamp(checksum: String, accessValidityOverride: Long? = null): Pair<String, Long> {
        val expirationTimestamp = DateTimeUtils.now() + (accessValidityOverride ?: ACCESS_VALIDITY_SECONDS)
        val hmacBytes = getHmacBytes(checksum, expirationTimestamp)
        val signature = Base64.UrlSafe.encode(hmacBytes).trimEnd('=')
        return Pair(signature, expirationTimestamp)
    }

    /**
     * Validates a signature.
     * @param checksum The checksum
     * @param expirationTimestamp The expiration timestamp (Unix seconds)
     * @param providedSignature The provided signature to validate
     * @return true if signature is valid and not expired, false otherwise
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun validateSignature(checksum: String, expirationTimestamp: Long, providedSignature: String): Boolean {

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