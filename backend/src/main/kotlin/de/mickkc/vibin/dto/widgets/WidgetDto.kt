package de.mickkc.vibin.dto.widgets

import kotlinx.serialization.Serializable

@Serializable
data class WidgetDto(
    val id: String,
    val types: List<String>,
    val bgColor: Int?,
    val fgColor: Int?,
    val accentColor: Int?,
)