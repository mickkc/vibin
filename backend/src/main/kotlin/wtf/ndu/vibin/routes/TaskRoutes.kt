package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.tasks.TaskManager
import wtf.ndu.vibin.tasks.TaskScheduler

fun Application.configureTaskRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/tasks", PermissionType.MANAGE_TASKS) {
            val tasks = TaskManager.getTasks()
            call.respond(tasks.map { it.toDto() })
        }

        putP("/api/tasks/{taskId}/enable", PermissionType.MANAGE_TASKS) {
            val taskId = call.parameters["taskId"] ?: return@putP call.missingParameter("taskId")
            val enable = call.parameters["enable"] ?: return@putP call.missingParameter("enable")

            val enableBool = enable.toBooleanStrictOrNull() ?: return@putP call.invalidParameter("enable", "true", "false")

            TaskManager.setTaskEnabled(taskId, enableBool)
            call.success()
        }

        postP("/api/tasks/{taskId}/run", PermissionType.MANAGE_TASKS) {
            val taskId = call.parameters["taskId"] ?: return@postP call.missingParameter("taskId")

            val success = TaskScheduler.forceRunTask(taskId)

            if (!success) {
                return@postP call.notFound()
            }

            call.success()
        }
    }

}