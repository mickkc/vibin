package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class PermissionUpdateDto(

    @JsonSchema.Description("Whether the permission was granted (true) or revoked (false).")
    @JsonSchema.Example("true")
    val granted: Boolean
)
