package de.mickkc.vibin.dto.widgets

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class CreateWidgetDto (

    @JsonSchema.Example("[\"USER\", \"ACTIVITY\"]")
    val types: List<String>,

    @JsonSchema.Example("\"000000\"")
    @JsonSchema.Description("A hex color with or without the leading #")
    val bgColor: String?,

    @JsonSchema.Example("\"ffffff\"")
    @JsonSchema.Description("A hex color with or without the leading #")
    val fgColor: String?,

    @JsonSchema.Example("\"ff0000\"")
    @JsonSchema.Description("A hex color with or without the leading #")
    val accentColor: String?
)