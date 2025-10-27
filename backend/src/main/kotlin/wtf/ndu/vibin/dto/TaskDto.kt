package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    val enabled: Boolean,
    val lastRun: Long?,
    val nextRun: Long?
)
