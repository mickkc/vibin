package de.mickkc.vibin.scrobbling

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ScrobblerStatus(
    @JsonSchema.Example("true")
    val error: Boolean,
    @JsonSchema.Example("\"Some error explaining why scrobbling failed.\"")
    val message: String?
) {
    companion object {
        fun error(message: String) = ScrobblerStatus(true, message)
        fun success() = ScrobblerStatus(false, null)
    }
}
