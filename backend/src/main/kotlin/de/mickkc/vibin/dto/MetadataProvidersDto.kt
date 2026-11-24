package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class MetadataProvidersDto(

    @JsonSchema.Example("[\"Metadata\"]")
    val file: Set<String>,

    @JsonSchema.Example("[\"Spotify\", \"iTunes\"]")
    val track: Set<String>,

    @JsonSchema.Example("[\"Spotify\", \"Last.fm\"]")
    val artist: Set<String>,

    @JsonSchema.Example("[\"Spotify\", \"Deezer\"]")
    val album: Set<String>,

    @JsonSchema.Example("[\"LrcLib\"]")
    val lyrics: Set<String>,

    @JsonSchema.Example("[\"JAudioTagger\", \"ffprobe\"]")
    val preParsers: Set<String>
)
