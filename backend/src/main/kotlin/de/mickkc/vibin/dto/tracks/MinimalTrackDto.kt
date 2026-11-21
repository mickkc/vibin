package de.mickkc.vibin.dto.tracks

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.IdNameDto
import io.ktor.openapi.JsonSchema

@Serializable
data class MinimalTrackDto (

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"Never Gonna Give You Up\"")
    val title: String,

    @JsonSchema.Example("{\"id\": 1, \"name\": \"Rick Astley\"}")
    val artists: List<IdNameDto>,

    val album: IdNameDto,

    @JsonSchema.Example("1")
    val coverId: Long?,

    @JsonSchema.Example("215000")
    @JsonSchema.Description("Track duration in milliseconds")
    val duration: Long?,

    @JsonSchema.Example("{\"id\": 1, \"name\": \"Mickkc\"}")
    val uploader: IdNameDto?
)