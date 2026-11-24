package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable
import de.mickkc.vibin.tasks.TaskResult
import io.ktor.openapi.JsonSchema

@Serializable
data class TaskDto(

    @JsonSchema.Example("\"scan_for_new_tracks\"")
    val id: String,

    @JsonSchema.Example("true")
    val enabled: Boolean,

    @JsonSchema.Example("1782511260")
    val lastRun: Long?,

    @JsonSchema.Example("1782511260")
    val nextRun: Long?,

    @JsonSchema.Example("1782511260")
    val interval: Long?,

    val lastResult: TaskResult?
)
