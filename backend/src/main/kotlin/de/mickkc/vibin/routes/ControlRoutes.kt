package de.mickkc.vibin.routes

import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.SessionPlayState
import de.mickkc.vibin.repos.SessionRepeatState
import de.mickkc.vibin.repos.SetQueueRequest
import de.mickkc.vibin.websockets.Session
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun RoutingCall.getControlSession(paramName: String = "id"): Session {
    val userId = getUserId() ?: unauthorized()
    val sessionId = getStringParameter(paramName)

    if (sessionId.trim().lowercase() == "current") {
        val currentSession = ControlRepo.getCurrentSession(userId) ?: notFound()
        return currentSession
    }

    val session = ControlRepo.getById(sessionId) ?: notFound()

    if (userId != session.userId) {
        unauthorized()
    }

    return session
}

fun Application.configureControlRoutes() = routing {

    authenticate("tokenAuth") {
        get("/api/control/sessions") {
            val userId = call.getUserId() ?: unauthorized()
            val sessions = ControlRepo.getAllForUser(userId)
            call.respond(ControlRepo.toDto(sessions))
        }

        get("/api/control/sessions/{id}") {
            val session = call.getControlSession()
            call.respond(ControlRepo.toDto(session))
        }

        post("/api/control/sessions/{id}/play") {
            val session = call.getControlSession()
            val playing = session.sendState(SessionPlayState.PLAYING)
            call.success(playing)
        }

        post("/api/control/sessions/{id}/pause") {
            val session = call.getControlSession()
            val paused = session.sendState(SessionPlayState.PAUSED)
            call.success(paused)
        }

        post("/api/control/sessions/{id}/stop") {
            val session = call.getControlSession()
            val stopped = session.sendState(SessionPlayState.STOPPED)
            call.success(stopped)
        }

        post("/api/control/sessions/{id}/track") {
            val session = call.getControlSession()
            val trackId = call.getLongParameter("trackId")
            val updated = session.sendCurrentTrackId(trackId)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/queue/track") {
            val session = call.getControlSession()
            val trackPosition = call.getIntParameter("pos")
            val updated = session.sendCurrentQueueTrackId(trackPosition)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/repeat") {
            val session = call.getControlSession()
            val state = call.getEnum<SessionRepeatState>("state", null)
            val updated = session.sendRepeatState(state)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/shuffle") {
            val session = call.getControlSession()
            val state = call.getBooleanParameter("state")
            val updated = session.sendShuffleState(state)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/volume") {
            val session = call.getControlSession()
            val volume = call.getFloatParameter("volume")

            if (volume !in 0f..1f) {
                invalidParameter("volume",)
            }

            val updated = session.sendVolume(volume)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/speed") {
            val session = call.getControlSession()
            val speed = call.getFloatParameter("speed")

            if (speed !in 0.1f..2f) {
                invalidParameter("speed")
            }

            val updated = session.sendSpeed(speed)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/queue") {
            val session = call.getControlSession()
            val idsStr = call.getStringParameter("ids")
            val audioType = call.parameters["audioType"]
            val audioTypeId = call.parameters["audioTypeId"]?.toLongOrNull()
            val shuffle = call.parameters["shuffle"]?.toBooleanStrictOrNull()
            val startId = call.parameters["startId"]?.toLongOrNull()

            val ids = idsStr.split(',').mapNotNull { it.trim().toLongOrNull() }

            val updated = session.sendQueue(SetQueueRequest(
                ids, startId, audioType, audioTypeId, shuffle
            ))

            call.success(updated)
        }
    }
}