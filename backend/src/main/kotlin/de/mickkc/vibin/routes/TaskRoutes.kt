package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.tasks.TaskManager
import de.mickkc.vibin.tasks.TaskScheduler

fun Application.configureTaskRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/tasks", PermissionType.MANAGE_TASKS) {
            val tasks = TaskManager.getTasks()
            call.respond(tasks.map { it.toDto() })
        }

        putP("/api/tasks/{taskId}/enable", PermissionType.MANAGE_TASKS) {
            val taskId = call.getStringParameter("taskId") ?: return@putP
            val enable = call.getBooleanParameter("enable") ?: return@putP

            if (TaskManager.getById(taskId) == null) {
                return@putP call.notFound()
            }

            TaskManager.setTaskEnabled(taskId, enable)
            call.success()
        }

        postP("/api/tasks/{taskId}/run", PermissionType.MANAGE_TASKS) {
            val taskId = call.getStringParameter("taskId") ?: return@postP

            val result = TaskScheduler.forceRunTask(taskId) ?: return@postP call.notFound()

            call.respond(result)
        }
    }

}