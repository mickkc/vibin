package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"MyLaptop\"")
    val deviceName: String,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val lastUsed: Long
)
