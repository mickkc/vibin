package de.mickkc.vibin.dto.playlists

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.IdNameDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import io.ktor.openapi.JsonSchema

@Serializable
data class PlaylistTrackDto (
    val track: MinimalTrackDto,

    @JsonSchema.Example("0")
    val position: Int,

    @JsonSchema.Example("{\"id\": 1, \"name\": \"Mickkc\"}")
    val addedBy: IdNameDto?,

    @JsonSchema.Example("1782511260")
    val addedAt: Long?
)