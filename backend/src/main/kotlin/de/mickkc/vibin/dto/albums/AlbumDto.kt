package de.mickkc.vibin.dto.albums

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.ArtistDto

@Serializable
data class AlbumDto (
    val id: Long,
    val title: String,
    val description: String,
    val year: Int?,
    val artists: List<ArtistDto>,
    val trackCount: Long,
    val single: Boolean,
    val createdAt: Long,
    val updatedAt: Long?
)