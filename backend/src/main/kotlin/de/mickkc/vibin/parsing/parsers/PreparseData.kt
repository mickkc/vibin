package de.mickkc.vibin.parsing.parsers

import java.io.File

data class PreparseData(
    val durationMs: Long,
    val bitrate: String?,
    val sampleRate: String?,
    val channels: String?,
    val file: File
) {

    fun getChannelsInt(): Int? {
        return when (channels?.lowercase()) {
            "mono" -> 1
            "stereo" -> 2
            else -> channels?.toIntOrNull()
        }
    }

}