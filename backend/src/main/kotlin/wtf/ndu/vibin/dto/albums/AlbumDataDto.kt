package wtf.ndu.vibin.dto.albums

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class AlbumDataDto(
    val album: AlbumDto,
    val tracks: List<MinimalTrackDto>
)