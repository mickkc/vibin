package wtf.ndu.vibin.dto.playlists

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.IdNameDto
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class PlaylistTrackDto (
    val track: MinimalTrackDto,
    val position: Int,
    val addedBy: IdNameDto?,
    val addedAt: Long?
)