package wtf.ndu.vibin.dto.albums

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto
import wtf.ndu.vibin.dto.tracks.TrackDto

@Serializable
data class AlbumDataDto(
    val album: AlbumDto,
    val tracks: List<TrackDto>
)