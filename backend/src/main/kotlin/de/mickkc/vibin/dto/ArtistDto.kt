package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long?
)
