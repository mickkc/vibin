package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class HealthCheckDto(

    @JsonSchema.Example("\"ok\"")
    @JsonSchema.Description("The application status. This will always be \"ok\".")
    val status: String,

    @JsonSchema.Example("\"0.1.1\"")
    @JsonSchema.Description("The current application version")
    val version: String,

    @JsonSchema.Example("[\"0.1.1\", \"0.1.2\"]")
    @JsonSchema.Description("A list of supported app versions")
    val supportedAppVersions: List<String>
)
