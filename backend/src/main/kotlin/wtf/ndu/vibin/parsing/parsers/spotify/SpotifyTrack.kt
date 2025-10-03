package wtf.ndu.vibin.parsing.parsers.spotify

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class SpotifyTrack(
    val name: String,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum,
    val disc_number: Int?,
    val track_number: Int?,
    val explicit: Boolean,
)

@Serializable
data class SpotifyTrackResponse(
    val tracks: SpotifyData<SpotifyTrack>
)