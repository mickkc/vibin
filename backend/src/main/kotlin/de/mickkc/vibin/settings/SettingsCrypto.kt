package de.mickkc.vibin.settings

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.SETTINGS_ENCRYPTION_KEY
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SettingsCrypto {

    private val key = Base64.getDecoder().decode(EnvUtil.getOrError(SETTINGS_ENCRYPTION_KEY))
    private val cipher get() = Cipher.getInstance("AES/GCM/NoPadding")

    fun encrypt(text: String): String {
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = cipher
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, nonce))
        val ct = c.doFinal(text.toByteArray(Charsets.UTF_8))
        return "enc:v1:${Base64.getEncoder().encodeToString(nonce)}:${Base64.getEncoder().encodeToString(ct)}"
    }

    fun decrypt(value: String): String {
        if (!value.startsWith("enc:v1:")) return value
        val parts = value.removePrefix("enc:v1:").split(":")
        val c = cipher
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, Base64.getDecoder().decode(parts[0])))
        return String(c.doFinal(Base64.getDecoder().decode(parts[1])), Charsets.UTF_8)
    }

    fun isEncrypted(value: String) = value.startsWith("enc:v1:")
}