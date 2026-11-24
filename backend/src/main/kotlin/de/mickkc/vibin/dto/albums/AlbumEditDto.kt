package de.mickkc.vibin.dto.albums

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class AlbumEditDto(

    @JsonSchema.Example("\"Whenever You Need Somebody\"")
    @JsonSchema.MaxLength(255)
    val title: String?,

    @JsonSchema.Example("\"https://example.org/images/example.png\"")
    val coverUrl: String?,

    @JsonSchema.Example("\"Whenever You Need Somebody is the debut studio album by English singer Rick Astley, released on 16 November 1987 by RCA Records.\"")
    val description: String?,

    @JsonSchema.Example("1987")
    val year: Int?,

    @JsonSchema.Example("false")
    val isSingle: Boolean?,

) : ValidatableBaseDto {
    override fun validate() {
        if (title != null && title.length > 255) invalidParameter("title", "title.length < 255")
    }
}
