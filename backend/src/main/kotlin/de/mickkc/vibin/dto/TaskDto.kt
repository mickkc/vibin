package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable
import de.mickkc.vibin.tasks.TaskResult

@Serializable
data class TaskDto(
    val id: String,
    val enabled: Boolean,
    val lastRun: Long?,
    val nextRun: Long,
    val interval: Long,
    val lastResult: TaskResult?
)
