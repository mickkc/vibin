package wtf.ndu.vibin.parsing.parsers.spotify

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class SpotifyAlbum (
    val name: String,
    val album_type: String,
    val images: List<SpotifyImage>,
    val artists: List<SpotifyAlbumArtist>,
    val release_date: String,
    val total_tracks: Int,
)

@Serializable
data class SpotifyAlbumResponse(
    val albums: SpotifyData<SpotifyAlbum>
)

@Serializable
data class SpotifyAlbumArtist(
    val name: String
)