package de.mickkc.vibin.routes

import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.ControlSession
import de.mickkc.vibin.repos.SessionPlayState
import de.mickkc.vibin.repos.SessionRepeatState
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

        post("/api/control/sessions/{id}/play") {
            val session = call.getControlSession() ?: return@post
            val playing = session.setState?.invoke(SessionPlayState.PLAYING) ?: false
            call.success(playing)
        }

        post("/api/control/sessions/{id}/pause") {
            val session = call.getControlSession() ?: return@post
            val paused = session.setState?.invoke(SessionPlayState.PAUSED) ?: false
            call.success(paused)
        }

        post("/api/control/sessions/{id}/stop") {
            val session = call.getControlSession() ?: return@post
            val stopped = session.setState?.invoke(SessionPlayState.STOPPED) ?: false
            call.success(stopped)
        }

        post("/api/control/sessions/{id}/track") {
            val session = call.getControlSession() ?: return@post
            val trackId = call.getLongParameter("trackId") ?: return@post
            val updated = session.setCurrentTrackId?.invoke(trackId) ?: false
            call.success(updated)
        }

        post("/api/control/sessions/{id}/queue/track") {
            val session = call.getControlSession() ?: return@post
            val trackPosition = call.getIntParameter("pos") ?: return@post
            val updated = session.setTrackIdFromQueue?.invoke(trackPosition) ?: false
            call.success(updated)
        }

        post("/api/control/sessions/{id}/queue") {
            val session = call.getControlSession() ?: return@post
            val queue = call.getStringParameter("queue") ?: return@post
            val index = call.getIntOrDefault("index", -1)

            try {
                val trackIds = queue.split(",").map { it.trim().toLong() }

                val updated = session.setQueue?.invoke(trackIds, if (index == -1) null else index) ?: false
                call.success(updated)
            }
            catch (_: NumberFormatException) {
                call.invalidParameter("queue")
            }
        }

        post("/api/control/sessions/{id}/repeat") {
            val session = call.getControlSession() ?: return@post
            val state = call.getEnum<SessionRepeatState>("state", null) ?: return@post
            val updated = session.setRepeatState?.invoke(state) ?: false
            call.success(updated)
        }

        post("/api/control/sessions/{id}/shuffle") {
            val session = call.getControlSession() ?: return@post
            val state = call.getBooleanParameter("state") ?: return@post
            val updated = session.setShuffleState?.invoke(state) ?: false
            call.success(updated)
        }

        post("/api/control/sessions/{id}/volume") {
            val session = call.getControlSession() ?: return@post
            val volume = call.getFloatParameter("volume") ?: return@post

            if (volume !in 0f..1f) {
                return@post call.invalidParameter("volume")
            }

            val updated = session.setVolume?.invoke(volume) ?: false
            call.success(updated)
        }

        post("/api/control/sessions/{id}/speed") {
            val session = call.getControlSession() ?: return@post
            val speed = call.getFloatParameter("speed") ?: return@post

            if (speed !in 0.1f..2f) {
                return@post call.invalidParameter("speed")
            }

            val updated = session.setSpeed?.invoke(speed) ?: false
            call.success(updated)
        }
    }
}