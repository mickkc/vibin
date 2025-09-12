package wtf.ndu.vibin.dto.playlists

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.tracks.TrackDto

@Serializable
data class PlaylistTrackDto (
    val track: TrackDto,
    val source: String
)