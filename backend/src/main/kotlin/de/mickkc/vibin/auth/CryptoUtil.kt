package de.mickkc.vibin.auth

import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.SessionEntity
import de.mickkc.vibin.db.SessionTable
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object CryptoUtil {

    const val ITERATIONS = 65536
    const val KEY_LENGTH = 256
    const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private val factory = SecretKeyFactory.getInstance(ALGORITHM)

    val secureRandom = SecureRandom()

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
    fun createToken(): String = transaction {
        var token: String
        do {
            token = generateSequence {
                (('A'..'Z') + ('a'..'z') + ('0'..'9')).random()
            }.take(64).joinToString("")
        } while (SessionEntity.find { SessionTable.token eq token }.any())
        return@transaction token
    }
}