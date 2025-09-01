package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class ImageDto(
    val originalUrl: String,
    val smallUrl: String,
    val largeUrl: String?,
    val colorScheme: ColorSchemeDto? = null
)