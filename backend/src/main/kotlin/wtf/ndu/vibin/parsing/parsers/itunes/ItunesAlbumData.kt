package wtf.ndu.vibin.parsing.parsers.itunes

import kotlinx.serialization.Serializable

@Serializable
data class ItunesAlbumData (
    val collectionName: String,
    val artistName: String,
    val releaseDate: String,
    val artworkUrl100: String?,
)