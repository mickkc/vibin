package wtf.ndu.vibin.parsing.parsers.spotify

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyArtist(
    val name: String,
    val images: List<SpotifyImage>,
)

@Serializable
data class SpotifyArtistResponse(
    val artists: SpotifyData<SpotifyArtist>
)