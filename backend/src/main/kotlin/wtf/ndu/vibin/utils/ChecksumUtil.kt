package wtf.ndu.vibin.utils

import java.io.File
import java.security.MessageDigest

object ChecksumUtil {

    private val digest = MessageDigest.getInstance("MD5")

    /**
     * Calculates the MD5 checksum of a file given its file path.
     *
     * @param filePath The path to the file.
     * @return The MD5 checksum as a hexadecimal string.
     */
    fun getChecksum(filePath: String): String {
        val file = File(filePath)
        return getChecksum(file)
    }

    /**
     * Calculates the MD5 checksum of a file.
     *
     * @param file The file to calculate the checksum for.
     * @return The MD5 checksum as a hexadecimal string.
     */
    fun getChecksum(file: File): String {
        val fileBytes = file.readBytes()
        return getChecksum(fileBytes)
    }

    /**
     * Calculates the MD5 checksum of a byte array.
     *
     * @param data The byte array to calculate the checksum for.
     * @return The MD5 checksum as a hexadecimal string.
     */
    fun getChecksum(data: ByteArray): String {
        val hashBytes = digest.digest(data)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}