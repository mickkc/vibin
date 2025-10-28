package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.tasks.TaskResult

@Serializable
data class TaskDto(
    val id: String,
    val enabled: Boolean,
    val lastRun: Long?,
    val nextRun: Long,
    val interval: Long,
    val lastResult: TaskResult?
)
