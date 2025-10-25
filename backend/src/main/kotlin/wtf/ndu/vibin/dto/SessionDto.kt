package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val id: Long,
    val createdAt: Long,
    val lastUsed: Long
)
