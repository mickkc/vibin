package de.mickkc.vibin.dto.albums

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.artists.ArtistDto
import io.ktor.openapi.JsonSchema

@Serializable
data class AlbumDto (

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"Whenever You Need Somebody\"")
    val title: String,

    @JsonSchema.Example("\"Whenever You Need Somebody is the debut studio album by English singer Rick Astley, released on 16 November 1987 by RCA Records.\"")
    val description: String,

    @JsonSchema.Example("1")
    val coverId: Long?,

    @JsonSchema.Example("1987")
    val year: Int?,

    val artists: List<ArtistDto>,

    @JsonSchema.Example("5")
    val trackCount: Long,

    @JsonSchema.Example("false")
    val single: Boolean,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val updatedAt: Long?
)