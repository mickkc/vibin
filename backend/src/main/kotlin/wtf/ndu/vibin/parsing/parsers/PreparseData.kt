package wtf.ndu.vibin.parsing.parsers

import org.jaudiotagger.audio.AudioFile

data class PreparseData(
    val durationMs: Long,
    val bitrate: String?,
    val sampleRate: String?,
    val channels: String?,
    val audioFile: AudioFile
)
