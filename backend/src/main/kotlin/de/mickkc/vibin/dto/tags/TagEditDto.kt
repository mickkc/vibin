package de.mickkc.vibin.dto.tags

import kotlinx.serialization.Serializable

@Serializable
data class TagEditDto(
    val name: String,
    val description: String?,
    val importance: Int
)
