package de.mickkc.vibin.dto.playlists

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.IdNameDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class PlaylistTrackDto (
    val track: MinimalTrackDto,
    val position: Int,
    val addedBy: IdNameDto?,
    val addedAt: Long?
)