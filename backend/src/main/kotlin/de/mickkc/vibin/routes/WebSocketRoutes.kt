package de.mickkc.vibin.routes

import com.google.gson.Gson
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.repos.ListenTrackingRepo
import de.mickkc.vibin.repos.SessionRepo
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Serializable
data class WsFrame(val type: String, val data: Map<String, @Serializable @Contextual Any>, val timestamp: Long)

fun Application.configureWebSocketRoutes() = routing {

    val parser = Gson()

    webSocket("/ws/playback") {

        val authToken = call.parameters["token"]
        if (authToken.isNullOrEmpty()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing auth token"))
            return@webSocket
        }

        val userId = SessionRepo.validateAndUpdateToken(authToken)
        if (userId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid auth token"))
            return@webSocket
        }

        val sessionId = CryptoUtil.createToken(16)

        val logger: Logger = LoggerFactory.getLogger("WebSocketPlayback-User-$userId-Session-$sessionId")
        logger.info("WebSocket connection established: ${this.call.request.local.remoteHost}")

        runCatching {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                val receivedText = frame.readText()
                logger.info("Received via WebSocket: $receivedText")

                val parsedFrame = parser.fromJson(receivedText, WsFrame::class.java)

                when (parsedFrame.type) {
                    "ping" -> {}
                    "finished_track" -> {
                        val trackId = (parsedFrame.data["trackId"] as? Double)?.toLong()
                        if (trackId != null) {
                            ListenTrackingRepo.trackFinished(sessionId, trackId, parsedFrame.timestamp)
                        }
                    }
                    "play" -> {
                        ListenTrackingRepo.trackResumed(sessionId, parsedFrame.timestamp)
                    }
                    "pause" -> {
                        ListenTrackingRepo.trackPaused(sessionId, parsedFrame.timestamp)
                    }
                    "stop" -> {
                        ListenTrackingRepo.stopped(sessionId, parsedFrame.timestamp)
                    }
                    "skipped_next" -> {
                        ListenTrackingRepo.trackSkipped(sessionId, parsedFrame.timestamp)
                    }
                    "skipped_prev" -> {
                        ListenTrackingRepo.trackSkipped(sessionId, parsedFrame.timestamp)
                    }
                    "started_track" -> {
                        val trackId = (parsedFrame.data["trackId"] as? Double)?.toLong()
                        if (trackId != null) {
                            ListenTrackingRepo.trackStarted(sessionId, userId, trackId, parsedFrame.timestamp)
                        }
                    }
                    "listen" -> {
                        val type = parsedFrame.data["type"] as? String
                        val id = (parsedFrame.data["id"] as? Double)?.toLong()
                        if (type != null && id != null) {
                            ListenRepo.listenedTo(
                                userId = userId,
                                entityId = id,
                                type = when (type) {
                                    "track" -> ListenType.TRACK
                                    "album" -> ListenType.ALBUM
                                    "playlist" -> ListenType.PLAYLIST
                                    "artist" -> ListenType.ARTIST
                                    else -> {
                                        logger.error("Unknown listen type: $type")
                                        return@consumeEach
                                    }
                                },
                                at = parsedFrame.timestamp
                            )
                        }
                    }
                    else -> {
                        logger.warn("Unknown WebSocket frame type: ${parsedFrame.type}")
                    }
                }
            }
        }.onFailure {
            logger.error("WebSocket error: ${it.localizedMessage}", it)
        }.also {
            ListenTrackingRepo.stopped(sessionId, ListenTrackingRepo.getNowWithOffset(sessionId))
            ListenTrackingRepo.deleteSessionData(sessionId)
            logger.info("WebSocket connection closed")
        }

    }
}