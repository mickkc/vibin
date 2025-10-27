package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class ImageDto(
    val smallUrl: String,
    val mediumUrl: String?,
    val largeUrl: String?,
    val colorScheme: ColorSchemeDto? = null
)