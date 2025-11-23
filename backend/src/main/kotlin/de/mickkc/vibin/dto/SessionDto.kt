package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val id: Long,
    val createdAt: Long,
    val lastUsed: Long
)
