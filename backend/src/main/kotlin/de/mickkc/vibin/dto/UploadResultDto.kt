package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class UploadResultDto(

    @JsonSchema.Example("true")
    val success: Boolean,

    @JsonSchema.Example("false")
    val didFileAlreadyExist: Boolean = false,

    @JsonSchema.Example("1")
    @JsonSchema.Description("The id of the created track")
    val id: Long? = null,
)
