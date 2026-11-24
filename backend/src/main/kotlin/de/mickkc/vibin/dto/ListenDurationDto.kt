package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ListenDurationDto(

    @JsonSchema.Example("138108")
    val duration: Long
)
