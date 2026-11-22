package de.mickkc.vibin

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class IsFavoriteDto(

    @JsonSchema.Example("true")
    val isFavorite: Boolean,

    @JsonSchema.Example("1")
    @JsonSchema.Description("The place of the favorite or null if it isn't a favorite")
    val place: Int?
)
