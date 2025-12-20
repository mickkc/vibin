package de.mickkc.vibin.dto.artists

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: Long,
    val name: String,
    val description: String,
    val imageId: Long?,
    val createdAt: Long,
    val updatedAt: Long?
)