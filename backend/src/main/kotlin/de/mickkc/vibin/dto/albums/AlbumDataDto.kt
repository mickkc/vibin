package de.mickkc.vibin.dto.albums

import de.mickkc.vibin.dto.tracks.TrackDto
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDataDto(
    val album: AlbumDto,
    val tracks: List<TrackDto>
)