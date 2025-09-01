package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class ColorSchemeDto (
    val primary: String,
    val light: String,
    val dark: String
)