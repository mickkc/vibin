package de.mickkc.vibin.routes

import com.google.gson.Gson
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.TaskDto
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
import io.ktor.server.routing.openapi.describe
import io.ktor.server.websocket.*
import io.ktor.utils.io.ExperimentalKtorApi
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

@OptIn(ExperimentalKtorApi::class)
fun Application.configureTaskRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/tasks", PermissionType.MANAGE_TASKS) {
            val tasks = TaskManager.getTasks()
            call.respond(tasks.map { it.toDto() })
        }.describe {
            operationId = "getAllTasks"
            summary = "Get all tasks"
            tag("Tasks")
            authResponses(PermissionType.MANAGE_TASKS) {
                successResponse<List<TaskDto>>("A list of all tasks")
            }
        }

        putP("/api/tasks/{taskId}/enable", PermissionType.MANAGE_TASKS) {
            val taskId = call.getStringParameter("taskId")
            val enable = call.getBooleanParameter("enable")

            if (TaskManager.getById(taskId) == null) {
                notFound()
            }

            TaskManager.setTaskEnabled(taskId, enable)
            call.success()
        }.describe {
            operationId = "setTaskEnabled"
            summary = "Enable or disable a task"
            tag("Tasks")
            parameters {
                p("taskId", "scan_for_new_tracks")
                q("enable", true, required = true)
            }
            authResponses(PermissionType.MANAGE_TASKS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Task")
            }
        }

        postP("/api/tasks/{taskId}/run", PermissionType.MANAGE_TASKS) {
            val taskId = call.getStringParameter("taskId")

            TaskScheduler.forceRunTask(taskId)

            call.success()
        }.describe {
            operationId = "runTask"
            summary = "Run a task now"
            tag("Tasks")
            parameters {
                p("taskId", "scan_for_new_tracks")
            }
            authResponses(PermissionType.MANAGE_TASKS) {
                successResponse<SuccessDto>("Success")
            }
        }

        deleteP("/api/tasks/{taskId}/run", PermissionType.MANAGE_TASKS) {
            val taskId = call.getStringParameter("taskId")

            TaskManager.cancelTask(taskId)

            call.success()
        }.describe {
            operationId = "cancelTask"
            summary = "Cancel a running task"
            tag("Tasks")
            parameters {
                p("taskId", "scan_for_new_tracks")
            }
            authResponses(PermissionType.MANAGE_TASKS) {
                successResponse<SuccessDto>("Success")
            }
        }

    }

    webSocket("/api/tasks/progress") {

        val token = call.getStringParameter("token")
        val userId = SessionRepo.validateAndUpdateToken(token) ?: unauthorized()

        if (!PermissionRepo.hasPermission(userId, PermissionType.MANAGE_TASKS)) {
            noPermission(PermissionType.MANAGE_TASKS)
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
    }.describe {
        operationId = "getTaskProgressWS"
        summary = "Get a live feed of all running tasks and their progress"
        description = "See: [Websocket docs](https://vibin.rip/docs/websockets#tasks)"
        tag("Tasks")
        parameters {
            q("token", "cChUdROIcwZjXYWA7J5R22bjuecl9mv0BYaIa2D5Vk2CFR9VDcgYqjwsw1q9a2Vs", required = true,
                description = "A session token")
        }
        authResponses(PermissionType.MANAGE_TASKS) {
            successResponse<String>("A websocket connection, see [Websocket docs](https://vibin.rip/docs/websockets#tasks)")
            unauthorizedResponse("Token is invalid.")
        }
    }
}