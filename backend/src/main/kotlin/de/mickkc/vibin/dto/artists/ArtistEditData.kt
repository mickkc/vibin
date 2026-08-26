package de.mickkc.vibin.dto.artists

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import kotlinx.serialization.Serializable

@Serializable
data class ArtistEditData (
    val name: String?,
    val description: String?,
    val imageUrl: String?
) : ValidatableBaseDto {
    override fun validate() {
        if (name != null && name.length > 255) invalidParameter("name", "name.length < 255")
    }
}