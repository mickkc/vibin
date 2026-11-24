package de.mickkc.vibin.dto.scrobbling

import de.mickkc.vibin.scrobbling.ScrobblerStatus
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ScrobblerDto (

    @JsonSchema.Example("\"last_fm\"")
    val id: String,

    @JsonSchema.Example("\"Last.fm\"")
    val name: String,

    @JsonSchema.Example("true")
    val available: Boolean,

    val status: ScrobblerStatus?
)