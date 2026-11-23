package de.mickkc.vibin.dto

import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.artists.ArtistDto
import de.mickkc.vibin.dto.tags.TagDto
import kotlinx.serialization.Serializable

@Serializable
data class CreatedMetadataDto(
    val artists: List<ArtistDto>,
    val tags: List<TagDto>,
    val album: AlbumDto?
)
