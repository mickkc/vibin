package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDataDto(
    val album: AlbumDto,
    val tracks: List<MinimalTrackDto>
)
