package de.mickkc.vibin.auth

import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CryptoUtil {

    const val ITERATIONS = 65536
    const val KEY_LENGTH = 256
    const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private val factory = SecretKeyFactory.getInstance(ALGORITHM)

    val secureRandom = SecureRandom()

    @Suppress("SuspiciousCallOnCollectionToAddOrRemovePath") // List items should explicitly be joined into one list
    private val tokenChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')

    fun getSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Generates a random token consisting of 64 alphanumeric characters that is guaranteed to be unique.
     * @return A randomly generated token string.
     */
    fun createToken(length: Int = 64): String = transaction {
        var token: String
        do {
            token = (1..length).map {
                tokenChars[secureRandom.nextInt(tokenChars.size)]
            }.joinToString("")
        } while (SessionEntity.find { SessionTable.token eq token }.any())
        return@transaction token
    }

    /**
     * Hashes a token using SHA-256 and returns the lowercase hex-encoded hash (64 characters).
     *
     * @param token The raw token to hash.
     * @return The SHA-256 hash of the token as a lowercase hex string.
     */
    fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}