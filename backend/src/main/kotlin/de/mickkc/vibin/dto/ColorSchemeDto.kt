package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ColorSchemeDto (

    @JsonSchema.Example("\"#ff0000\"")
    val primary: String,

    @JsonSchema.Example("\"#ffffff\"")
    val light: String,

    @JsonSchema.Example("\"#000000\"")
    val dark: String
)