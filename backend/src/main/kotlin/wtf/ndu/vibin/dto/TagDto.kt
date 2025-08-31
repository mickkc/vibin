package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class TagDto(
    val id: Long,
    val name: String,
    val color: String?,
    val createdAt: Long,
    val updatedAt: Long?
)
