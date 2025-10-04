package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.tags.TagDto

@Serializable
data class ArtistDto(
    val id: Long,
    val name: String,
    val image: ImageDto?,
    val sortName: String?,
    val tags: List<TagDto>,
    val createdAt: Long,
    val updatedAt: Long?
)
