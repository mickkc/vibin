package de.mickkc.vibin.dto.tags

import kotlinx.serialization.Serializable

@Serializable
data class TagDto(
    val id: Long,
    val name: String,
    val description: String,
    val importance: Int,
    val createdAt: Long,
    val updatedAt: Long?
)