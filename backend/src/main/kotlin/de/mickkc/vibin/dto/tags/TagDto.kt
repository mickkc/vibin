package de.mickkc.vibin.dto.tags

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class TagDto(

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"pop\"")
    val name: String,

    @JsonSchema.Example("\"Pop-Songs\"")
    val description: String,

    @JsonSchema.Example("10")
    @JsonSchema.Description("How likely tracks sharing this tag are being recommended, from 0 to 10.")
    @JsonSchema.Minimum(0.0)
    @JsonSchema.Maximum(10.0)
    val importance: Int,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val updatedAt: Long?
)