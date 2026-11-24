package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class LyricsDto(

    @JsonSchema.Example("\"[00:19.73]We're no strangers to love\n" +
            "[00:23.66]You know the rules and so do I" +
            "...\"")
    val lyrics: String?,

    val colorScheme: ColorSchemeDto?
)