package de.mickkc.vibin.parsing.parsers

import org.jaudiotagger.audio.AudioFile

data class PreparseData(
    val durationMs: Long,
    val bitrate: String?,
    val sampleRate: String?,
    val channels: String?,
    val audioFile: AudioFile
) {

    fun getChannelsInt(): Int? {
        return when (channels?.lowercase()) {
            "mono" -> 1
            "stereo" -> 2
            else -> channels?.toIntOrNull()
        }
    }

}