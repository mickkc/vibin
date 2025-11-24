package de.mickkc.vibin.dto.widgets

import kotlinx.serialization.Serializable

@Serializable
data class CreateWidgetDto (
    val types: List<String>,
    val bgColor: String?,
    val fgColor: String?,
    val accentColor: String?
)