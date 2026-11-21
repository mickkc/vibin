package de.mickkc.vibin.dto.artists

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ArtistDto(

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"Rick Astley\"")
    val name: String,

    @JsonSchema.Example("\"Richard Paul Astley (born 6 February 1966) is an English singer, songwriter, radio DJ and podcaster.\"")
    val description: String,

    @JsonSchema.Example("1")
    val imageId: Long?,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val updatedAt: Long?
)