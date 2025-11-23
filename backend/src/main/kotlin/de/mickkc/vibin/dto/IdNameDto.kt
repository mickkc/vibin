package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class IdNameDto(
    val id: Long,
    val name: String
)
