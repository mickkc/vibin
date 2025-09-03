package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class IdNameDto(
    val id: Long,
    val name: String
)
