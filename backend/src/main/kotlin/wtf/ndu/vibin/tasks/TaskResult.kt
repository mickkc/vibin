package wtf.ndu.vibin.tasks

import kotlinx.serialization.Serializable

@Serializable
data class TaskResult(
    val success: Boolean,
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
