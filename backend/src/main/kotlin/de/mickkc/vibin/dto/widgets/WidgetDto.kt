package de.mickkc.vibin.dto.widgets

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class WidgetDto(

    @JsonSchema.Example("1")
    val id: String,

    @JsonSchema.Example("[\"USER\", \"ACTIVITY\"]")
    val types: List<String>,

    @JsonSchema.Example("0")
    @JsonSchema.Description("Integer representation of the background color: 0x000000 -> 0")
    val bgColor: Int?,

    @JsonSchema.Example("16777215")
    @JsonSchema.Description("Integer representation of the foreground color: 0xffffff -> 16777215")
    val fgColor: Int?,

    @JsonSchema.Example("16711680")
    @JsonSchema.Description("Integer representation of the accent color: 0xff0000 -> 16711680")
    val accentColor: Int?,
)