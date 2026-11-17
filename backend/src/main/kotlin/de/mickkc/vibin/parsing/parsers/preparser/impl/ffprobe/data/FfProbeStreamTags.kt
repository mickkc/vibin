package de.mickkc.vibin.parsing.parsers.preparser.impl.ffprobe.data

import kotlinx.serialization.Serializable

@Serializable
data class FfProbeStreamTags(
    val ALBUM: String?,
    val album: String?,
    val ARTIST: String?,
    val artist: String?,
    val COMMENT: String?,
    val comment: String?,
    val DATE: String?,
    val date: String?,
    val disc: String?,
    val disctotal: String?,
    val track: String?,
    val tracktotal: String?,
    val TITLE: String?,
    val title: String?,
    val lyrics: String?
)
