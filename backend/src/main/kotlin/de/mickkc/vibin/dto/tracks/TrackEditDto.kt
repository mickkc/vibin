package de.mickkc.vibin.dto.tracks

import de.mickkc.vibin.db.tracks.TrackVisibility
import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class TrackEditDto(

    @JsonSchema.Example("\"Never Gonna Give You Up\"")
    @JsonSchema.MaxLength(255)
    val title: String?,

    @JsonSchema.Example("false")
    val explicit: Boolean?,

    @JsonSchema.Example("1")
    val trackNumber: Int?,

    @JsonSchema.Example("5")
    val trackCount: Int?,

    @JsonSchema.Example("1")
    val discNumber: Int?,

    @JsonSchema.Example("1")
    val discCount: Int?,

    @JsonSchema.Example("1987")
    val year: Int?,

    @JsonSchema.Example("\"Rickroll :D\"")
    val comment: String?,

    @JsonSchema.Example("\"https://example.org/images/example.png\"")
    val imageUrl: String?,

    @JsonSchema.Example("1")
    val album: Long?,

    @JsonSchema.Example("[1]")
    val artists: List<Long>?,

    @JsonSchema.Example("[1]")
    val tags: List<Long>?,

    @JsonSchema.Example("\"[00:19.73]We're no strangers to love\n" +
            "[00:23.66]You know the rules and so do I" +
            "...\"")
    val lyrics: String?,

    @JsonSchema.Example("\"Public\"")
    val visibility: TrackVisibility?

) : ValidatableBaseDto {
    override fun validate() {
        if (title != null && title.length > 255) invalidParameter("title", "title.length < 255")
    }
}