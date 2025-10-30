package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateMetadataDto(
    val artistNames: List<String>,
    val tagNames: List<String>,
    val albumName: String?
)
