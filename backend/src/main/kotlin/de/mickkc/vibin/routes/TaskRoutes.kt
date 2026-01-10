package de.mickkc.vibin.routes

import com.google.gson.Gson
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.tasks.TaskAction
import de.mickkc.vibin.tasks.TaskManager
import de.mickkc.vibin.tasks.TaskScheduler
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

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

            TaskScheduler.forceRunTask(taskId)

            call.success()
        }

        deleteP("/api/tasks/{taskId}/run", PermissionType.MANAGE_TASKS) {
            val taskId = call.getStringParameter("taskId") ?: return@deleteP

            TaskManager.cancelTask(taskId)

            call.success()
        }

    }

    webSocket("/api/tasks/progress") {

        val token = call.getStringParameter("token") ?: return@webSocket
        val userId = SessionRepo.validateAndUpdateToken(token) ?: return@webSocket call.unauthorized()

        if (!PermissionRepo.hasPermission(userId, PermissionType.MANAGE_TASKS)) {
            return@webSocket call.forbidden(PermissionType.MANAGE_TASKS)
        }

        val json = Gson().newBuilder().serializeNulls().create()
        val logger = LoggerFactory.getLogger("TaskProgressWebSocket")

        // Launch a coroutine to send progress updates
        val job = launch {
            TaskManager.sharedFlow.collect { message ->
                outgoing.send(Frame.Text(json.toJson(message)))
            }
        }

        try {

            // Send currently running progresses upon connection
            val runningProgresses = TaskManager.getRunningProgresses()
            for (progress in runningProgresses) {
                val action = TaskAction(progress.taskId, "start", progress = progress)
                outgoing.send(Frame.Text(json.toJson(action)))
            }

            // Listen for incoming messages
            for (frame in incoming) {
                if (frame is Frame.Close) {
                    break
                }
                else if (frame is Frame.Text) {
                    // Reply to pings
                    logger.info("Received message: ${frame.readText()}")
                    outgoing.send(frame)
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket error: ${e.message}", e)
        } finally {
            logger.info("WebSocket closed")
            job.cancel()
        }
    }

}