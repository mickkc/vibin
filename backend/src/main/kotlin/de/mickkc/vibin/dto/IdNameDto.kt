package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class IdNameDto(

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"Example name\"")
    val name: String
)
