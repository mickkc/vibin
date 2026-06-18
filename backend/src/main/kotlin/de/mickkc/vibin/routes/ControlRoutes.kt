package de.mickkc.vibin.routes

import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.ControlSession
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*

suspend fun RoutingCall.getControlSession(paramName: String = "id"): ControlSession? {
    val userId = getUserId() ?: return null.also { unauthorized() }
    val sessionId = getStringParameter(paramName) ?: return null
    val session = ControlRepo.getById(sessionId) ?: return null.also { notFound() }

    if (userId != session.userId) {
        unauthorized()
        return null
    }

    return session
}

fun Application.configureControlRoutes() = routing {

    authenticate("tokenAuth") {
        get("/api/control/sessions") {
            val userId = call.getUserId() ?: return@get call.unauthorized()
            val sessions = ControlRepo.getAllForUser(userId)
            call.respond(ControlRepo.toDto(sessions))
        }

        get("/api/control/sessions/{id}") {
            val session = call.getControlSession() ?: return@get
            call.respond(ControlRepo.toDto(session))
        }
    }
}