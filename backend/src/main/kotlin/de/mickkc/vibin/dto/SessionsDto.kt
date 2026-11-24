package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class SessionsDto(
    val sessions: List<SessionDto>,

    @JsonSchema.Description("The 0-based index of the current session inside the sessions list")
    @JsonSchema.Example("0")
    val currentSessionIndex: Int
)
