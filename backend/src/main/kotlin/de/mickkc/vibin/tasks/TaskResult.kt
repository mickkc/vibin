package de.mickkc.vibin.tasks

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class TaskResult(

    @JsonSchema.Example("false")
    val success: Boolean,

    @JsonSchema.Example("\"Some error explaining why the task failed.\"")
    val message: String? = null
) {
    companion object {
        fun success(message: String? = null): TaskResult {
            return TaskResult(true, message)
        }

        fun failure(message: String? = null): TaskResult {
            return TaskResult(false, message)
        }
    }
}
