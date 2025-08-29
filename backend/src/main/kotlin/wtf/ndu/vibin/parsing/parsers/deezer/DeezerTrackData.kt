package wtf.ndu.vibin.parsing.parsers.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerTrackData(
    val id: Long,
    val title: String,
    val duration: Double,
    val explicit_lyrics: Boolean,
    val artist: DeezerArtistData,
    val album: DeezerAlbumData
)

@Serializable
data class DeezerArtistData(
    val id: Long,
    val name: String
)

@Serializable
data class DeezerAlbumData(
    val id: Long,
    val title: String,
    val cover: String,
    val cover_small: String,
    val cover_medium: String,
    val cover_big: String,
    val cover_xl: String
)