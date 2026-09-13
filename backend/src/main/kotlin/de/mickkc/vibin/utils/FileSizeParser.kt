package de.mickkc.vibin.utils

object FileSizeParser {

    private val sizePattern = Regex("(?i)^\\s*(\\d+(?:\\.\\d+)?)\\s*([KMGT]?)B?\\s*$")

    /**
     * Parses a file size into a byte count:
     *
     * 1(B) -> 1 byte
     * 2K(B) -> 2 kilobyte
     * 3M(B) -> 3 megabyte
     * 4G(B) -> 4 gigabyte
     * 5T(B) -> 5 terrabyte
     *
     * @param s The file size string
     * @return The byte count or null for malformed input
     */
    fun parseFileSizeToBytes(s: String): Long? {
        val match = sizePattern.find(s) ?: return null
        val (amount, unit) = match.destructured
        val value = amount.toDoubleOrNull() ?: return null
        val multiplier = when (unit.uppercase()) {
            "K" -> 1024.0
            "M" -> 1024.0 * 1024
            "G" -> 1024.0 * 1024 * 1024
            "T" -> 1024.0 * 1024 * 1024 * 1024
            else -> 1.0
        }
        return (value * multiplier).toLong()
    }

}