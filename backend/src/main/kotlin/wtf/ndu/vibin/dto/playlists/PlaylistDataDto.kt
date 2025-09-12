package wtf.ndu.vibin.dto.playlists

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDataDto(
    val playlist: PlaylistDto,
    val tracks: List<PlaylistTrackDto>
)
