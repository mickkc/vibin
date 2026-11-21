package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class TotpStatusDto(

    @JsonSchema.Enum("None", "Totp", "Mail")
    @JsonSchema.Example("\"Mail\"")
    val status: String,

    @JsonSchema.Example("\"qfCrKkzXqUF5p3JwfOhg0jM99qBYAC2d\"")
    @JsonSchema.Description("Null if Totp is already configured")
    val secret: String?
)