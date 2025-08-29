package wtf.ndu.vibin.parsing.parsers.itunes

import kotlinx.serialization.Serializable

@Serializable
data class ItunesTrackData(
    val trackName: String,
    val artistName: String,
    val collectionName: String?,
    val trackTimeMillis: Long,
    val trackExplicitness: String,
    val artworkUrl100: String?,
    val discNumber: Int?,
    val discCount: Int?,
    val trackNumber: Int?,
    val trackCount: Int?,
    val primaryGenreName: String?,
    val releaseDate: String?
)
