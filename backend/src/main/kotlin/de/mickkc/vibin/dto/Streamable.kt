package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class Streamable(
    @JsonSchema.Example("1")
    val trackId: Long,

    @JsonSchema.Example("\"Never Gonna Give You Up\"")
    val title: String,

    @JsonSchema.Example("\"Rick Astley\"")
    val artist: String,

    @JsonSchema.Example("\"Whenever You Need Somebody\"")
    val album: String,

    @JsonSchema.Example("215000")
    @JsonSchema.Description("Track duration in milliseconds")
    val duration: Long?,

    @JsonSchema.Example("1.2")
    val volumeOffset: Double?,

    @JsonSchema.Example("\"9cdfb439c7876e703e307864c9167a15\"")
    val coverChecksum: String?,

    @JsonSchema.Example("\"7QlNYUkZoFXnjcGR+2WLmw57JNDQXrQhIR7s3DfrtWY=\"")
    val coverSignature: String?,

    @JsonSchema.Example("1782511260")
    val coverExpiration: Long?,

    @JsonSchema.Example("\"7QlNYUkZoFXnjcGR+2WLmw57JNDQXrQhIR7s3DfrtWY=\"")
    val streamSignature: String,

    @JsonSchema.Example("1782511260")
    val streamExpiration: Long
)
