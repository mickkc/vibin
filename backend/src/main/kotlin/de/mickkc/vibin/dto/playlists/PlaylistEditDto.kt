package de.mickkc.vibin.dto.playlists

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistEditDto (

    @JsonSchema.Example("\"Example Playlist\"")
    @JsonSchema.MaxLength(255)
    val name: String,

    @JsonSchema.Example("\"Some description\"")
    val description: String? = null,

    @JsonSchema.Example("true")
    val isPublic: Boolean? = null,

    @JsonSchema.Example("\"https://example.org/images/example.png\"")
    val coverImageUrl: String? = null,

    @JsonSchema.Example("[1,2,3]")
    val collaboratorIds: List<Long>? = null,

    @JsonSchema.Example("\"+pop\"")
    val vibedef: String? = null

) : ValidatableBaseDto {
    override fun validate() {
        if (name.length > 255)
            invalidParameter("name", "name.length <= 255")
    }
}