package wtf.ndu.vibin.parsing.parsers.lrclib

import kotlinx.serialization.Serializable

@Serializable
data class LrcLibLyrics(
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val duration: Double
)
