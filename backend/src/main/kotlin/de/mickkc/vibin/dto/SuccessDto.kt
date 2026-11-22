package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class SuccessDto(
    @JsonSchema.Example("true")
    val success: Boolean
)
