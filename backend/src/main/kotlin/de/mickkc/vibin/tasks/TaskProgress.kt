package de.mickkc.vibin.tasks

import kotlinx.serialization.Serializable

@Serializable
data class TaskProgress(
    val taskId: String,
    val progress: Double? = null,
    val message: String? = null,
)