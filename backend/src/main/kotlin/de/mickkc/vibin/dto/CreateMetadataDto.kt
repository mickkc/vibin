package de.mickkc.vibin.dto

import de.mickkc.vibin.routes.invalidParameter
import kotlinx.serialization.Serializable

@Serializable
data class CreateMetadataDto(
    val artistNames: List<String>,
    val tagNames: List<String>,
    val albumName: String?
) : ValidatableBaseDto {
    override fun validate() {
        if (artistNames.any { it.length > 255 })
            invalidParameter("artistNames", "artistName.length <= 255")

        if (tagNames.any { it.length > 255 })
            invalidParameter("tagNames", "tagName <= 255")

        if (albumName != null && albumName.length > 255)
            invalidParameter("albumName", "albumName.length <= 255")
    }
}