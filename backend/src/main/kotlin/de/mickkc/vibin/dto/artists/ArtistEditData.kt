package de.mickkc.vibin.dto.artists

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ArtistEditData (

    @JsonSchema.Example("\"Rick Astley\"")
    @JsonSchema.MaxLength(255)
    val name: String?,

    @JsonSchema.Example("\"Richard Paul Astley (born 6 February 1966) is an English singer, songwriter, radio DJ and podcaster.\"")
    val description: String?,

    @JsonSchema.Example("\"https://example.org/images/example.png\"")
    val imageUrl: String?

) : ValidatableBaseDto {
    override fun validate() {
        if (name != null && name.length > 255) invalidParameter("name", "name.length < 255")
    }
}