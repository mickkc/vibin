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

suspend fun RoutingCall.getControlSession(paramName: String = "id"): Session? {
    val userId = getUserId() ?: return null.also { unauthorized() }
    val sessionId = getStringParameter(paramName) ?: return null

    if (sessionId.trim().lowercase() == "current") {
        val currentSession = ControlRepo.getCurrentSession(userId)
        if (currentSession == null) notFound()
        return currentSession
    }

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
            val playing = session.sendState(SessionPlayState.PLAYING)
            call.success(playing)
        }

        post("/api/control/sessions/{id}/pause") {
            val session = call.getControlSession() ?: return@post
            val paused = session.sendState(SessionPlayState.PAUSED)
            call.success(paused)
        }

        post("/api/control/sessions/{id}/stop") {
            val session = call.getControlSession() ?: return@post
            val stopped = session.sendState(SessionPlayState.STOPPED)
            call.success(stopped)
        }

        post("/api/control/sessions/{id}/track") {
            val session = call.getControlSession() ?: return@post
            val trackId = call.getLongParameter("trackId") ?: return@post
            val updated = session.sendCurrentTrackId(trackId)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/queue/track") {
            val session = call.getControlSession() ?: return@post
            val trackPosition = call.getIntParameter("pos") ?: return@post
            val updated = session.sendCurrentQueueTrackId(trackPosition)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/repeat") {
            val session = call.getControlSession() ?: return@post
            val state = call.getEnum<SessionRepeatState>("state", null) ?: return@post
            val updated = session.sendRepeatState(state)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/shuffle") {
            val session = call.getControlSession() ?: return@post
            val state = call.getBooleanParameter("state") ?: return@post
            val updated = session.sendShuffleState(state)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/volume") {
            val session = call.getControlSession() ?: return@post
            val volume = call.getFloatParameter("volume") ?: return@post

            if (volume !in 0f..1f) {
                return@post call.invalidParameter("volume")
            }

            val updated = session.sendVolume(volume)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/speed") {
            val session = call.getControlSession() ?: return@post
            val speed = call.getFloatParameter("speed") ?: return@post

            if (speed !in 0.1f..2f) {
                return@post call.invalidParameter("speed")
            }

            val updated = session.sendSpeed(speed)
            call.success(updated)
        }

        post("/api/control/sessions/{id}/queue") {
            val session = call.getControlSession() ?: return@post
            val idsStr = call.getStringParameter("ids") ?: return@post
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