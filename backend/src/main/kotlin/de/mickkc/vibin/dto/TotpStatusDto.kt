package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class TotpStatusDto(
    val status: String,
    val secret: String?
)