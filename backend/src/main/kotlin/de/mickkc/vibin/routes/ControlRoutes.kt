package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.ControlSessionDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.SessionPlayState
import de.mickkc.vibin.repos.SessionRepeatState
import de.mickkc.vibin.repos.SetQueueRequest
import de.mickkc.vibin.websockets.Session
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

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

@OptIn(ExperimentalKtorApi::class)
fun Application.configureControlRoutes() = routing {

    authenticate("tokenAuth") {
        get("/api/control/sessions") {
            val userId = call.getUserId() ?: unauthorized()
            val sessions = ControlRepo.getAllForUser(userId)
            call.respond(ControlRepo.toDto(sessions))
        }.describe {
            operationId = "getPlaybackSessions"
            summary = "Get playback sessions"
            description = "Get all playback sessions that can be remotely controlled."
            tag("Control")
            authResponses {
                successResponse<List<ControlSessionDto>>("A list of playback sessions")
            }
        }

        get("/api/control/sessions/{id}") {
            val session = call.getControlSession()
            call.respond(ControlRepo.toDto(session))
        }.describe {
            operationId = "getPlaybackSession"
            summary = "Get a playback session by Id"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
            }
            authResponses {
                successResponse<ControlSessionDto>("The playback session")
                notFoundResponse("Playback session")
            }
        }

        post("/api/control/sessions/{id}/play") {
            val session = call.getControlSession()
            val playing = session.sendState(SessionPlayState.PLAYING)
            call.success(playing)
        }.describe {
            operationId = "resumePlayback"
            summary = "Start or resume a playback session"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }

        post("/api/control/sessions/{id}/pause") {
            val session = call.getControlSession()
            val paused = session.sendState(SessionPlayState.PAUSED)
            call.success(paused)
        }.describe {
            operationId = "pausePlayback"
            summary = "Pause a playback session"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }

        post("/api/control/sessions/{id}/stop") {
            val session = call.getControlSession()
            val stopped = session.sendState(SessionPlayState.STOPPED)
            call.success(stopped)
        }.describe {
            operationId = "stopPlayback"
            summary = "Stop a playback session"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }

        post("/api/control/sessions/{id}/track") {
            val session = call.getControlSession()
            val trackId = call.getLongParameter("trackId")
            val updated = session.sendCurrentTrackId(trackId)
            call.success(updated)
        }.describe {
            operationId = "updatePlaybackTrack"
            summary = "Override the currently playing track of a playback session"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("trackId", 1L)
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }

        post("/api/control/sessions/{id}/queue/track") {
            val session = call.getControlSession()
            val trackPosition = call.getIntParameter("pos")
            val updated = session.sendCurrentQueueTrackId(trackPosition)
            call.success(updated)
        }.describe {
            operationId = "updatePlaybackTrackFromQueue"
            summary = "Override the currently playing track of a playback session based on a position inside the queue"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("pos", 0L, description = "The position of the track inside the queue")
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }

        post("/api/control/sessions/{id}/repeat") {
            val session = call.getControlSession()
            val state = call.getEnum<SessionRepeatState>("state", null)
            val updated = session.sendRepeatState(state)
            call.success(updated)
        }.describe {
            operationId = "setPlaybackRepeatState"
            summary = "Change the repeat mode of a playback state"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("state", SessionRepeatState.ALL)
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }


        post("/api/control/sessions/{id}/shuffle") {
            val session = call.getControlSession()
            val state = call.getBooleanParameter("state")
            val updated = session.sendShuffleState(state)
            call.success(updated)
        }.describe {
            operationId = "setPlaybackShuffleState"
            summary = "Change the shuffle mode of a playback state"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("state", true)
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }

        post("/api/control/sessions/{id}/volume") {
            val session = call.getControlSession()
            val volume = call.getFloatParameter("volume")

            if (volume !in 0f..1f) {
                invalidParameter("volume",)
            }

            val updated = session.sendVolume(volume)
            call.success(updated)
        }.describe {
            operationId = "setPlaybackVolume"
            summary = "Change the volume of a playback state"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("volume", 1f)
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
                invalidParameterResponse("volume", "Volume is below 0 or above 1.")
            }
        }

        post("/api/control/sessions/{id}/speed") {
            val session = call.getControlSession()
            val speed = call.getFloatParameter("speed")

            if (speed !in 0.1f..2f) {
                invalidParameter("speed")
            }

            val updated = session.sendSpeed(speed)
            call.success(updated)
        }.describe {
            operationId = "setPlaybackSpeed"
            summary = "Change the speed of a playback state"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("volume", 1f)
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
                invalidParameterResponse("speed", "Speed is below 0.1 or above 2.")
            }
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
        }.describe {
            operationId = "setPlaybackQueue"
            summary = "Change the queue of a playback state"
            tag("Control")
            parameters {
                p("id", "970e6075a413f042")
                q("ids", "1,3,6,5", required = true, description = "Track Ids")
                q("audioType", "album", required = false,
                    description = "The source where the tracks come from: `album`, `playlist`, `artist`, `unspecified` or `null`.")
                q("audioTypeId", 1L, required = false,
                    description = "The Id of the source album/playlist/artist")
                q("shuffle", false, required = false,
                    description = "New shuffle mode.\n" +
                            "- `true`: Order will be randomized before playing, shuffling will be enabled.\n" +
                            "- `false`: Queue will be set as-is, shuffling will be disabled.\n" +
                            "- `null`: Leave shuffling as it was before.")
                q("startId", 1L, required = false,
                    description = "The Id of the first track. When shuffling is/gets enabled, this will still be the first track.")
            }
            authResponses {
                successResponse<SuccessDto>("True if the event was sent, false on errors.")
            }
        }
    }
}