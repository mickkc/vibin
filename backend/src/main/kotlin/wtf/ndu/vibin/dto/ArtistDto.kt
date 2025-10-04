package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(
    val id: Long,
    val name: String,
    val image: ImageDto?,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long?
)
