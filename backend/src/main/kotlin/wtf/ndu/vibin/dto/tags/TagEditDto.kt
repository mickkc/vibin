package wtf.ndu.vibin.dto.tags

import kotlinx.serialization.Serializable

@Serializable
data class TagEditDto(
    val name: String,
    val description: String?,
    val color: String?
)
