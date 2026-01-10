package de.mickkc.vibin.tasks

import kotlinx.serialization.Serializable

@Serializable
data class TaskAction(
    val taskId: String,
    val action: String,
    val progress: TaskProgress? = null,
    val result: TaskResult? = null,
)
