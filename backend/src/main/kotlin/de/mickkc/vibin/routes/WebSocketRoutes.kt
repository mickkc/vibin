package de.mickkc.vibin.routes

import com.google.gson.Gson
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.ControlSessionQueueItemInDto
import de.mickkc.vibin.repos.*
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Serializable
data class WsFrame(val type: String, val data: Map<String, @Serializable @Contextual Any?>, val timestamp: Long)

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

        val gson = Gson()

        suspend fun sendData(type: String, data: Map<String, @Serializable @Contextual Any?>): Boolean = withContext(Dispatchers.IO) {
            try {
                outgoing.send(Frame.Text(gson.toJson(WsFrame(type, data, DateTimeUtils.now()))))
                return@withContext true
            }
            catch (e: Exception) {
                logger.error("Error sending event to websocket.", e)
                return@withContext false
            }
        }

        ControlRepo.add(sessionId, userId) { session ->
            session.setState = {
                sendData("state", mapOf("state" to it.name))
            }
            session.setCurrentTrackId = {
                sendData("currentTrack", mapOf("id" to it))
            }
            session.setTrackIdFromQueue = {
                sendData("currentTrack", mapOf("queueId" to it))
            }
            session.setQueue = {
                sendData("queue", mapOf(
                    "ids" to it.ids,
                    "startId" to it.startId,
                    "audioType" to it.audioType,
                    "audioTypeId" to it.audioTypeId,
                    "shuffle" to it.shuffle
                ))
            }
            session.setShuffleState = { state ->
                sendData("shuffle", mapOf("state" to state))
            }
            session.setRepeatState = { state ->
                sendData("repeat", mapOf("state" to state))
            }
            session.setVolume = { volume ->
                sendData("volume", mapOf("volume" to volume))
            }
            session.setSpeed = { speed ->
                sendData("speed", mapOf("speed" to speed))
            }
        }

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
                        ControlRepo.setState(sessionId, SessionPlayState.PLAYING)
                    }
                    "pause" -> {
                        ListenTrackingRepo.trackPaused(sessionId, parsedFrame.timestamp)
                        ControlRepo.setState(sessionId, SessionPlayState.PAUSED)
                    }
                    "stop" -> {
                        ListenTrackingRepo.stopped(sessionId, parsedFrame.timestamp)
                        ControlRepo.setState(sessionId, SessionPlayState.STOPPED)
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
                            ControlRepo.setIsCurrentTrackId(sessionId, trackId)
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
                    "repeat" -> {
                        val mode = parsedFrame.data["mode"] as? String
                        if (mode == null) {
                            logger.error("Invalid repeat mode in websocket message.")
                        }
                        else {
                            when (mode) {
                                "all" -> ControlRepo.setRepeatState(sessionId, SessionRepeatState.ALL)
                                "one" -> ControlRepo.setRepeatState(sessionId, SessionRepeatState.CURRENT_TRACK)
                                "off" -> ControlRepo.setRepeatState(sessionId, SessionRepeatState.OFF)
                                else -> logger.warn("Invalid repeat mode: $mode")
                            }
                        }
                    }
                    "shuffle" -> {
                        val mode = parsedFrame.data["shuffle"] as? Boolean
                        if (mode == null) {
                            logger.error("Invalid shuffle mode in websocket message.")
                        }
                        else {
                            ControlRepo.setShuffleState(sessionId, mode)
                        }
                    }
                    "volume" -> {
                        val volume = parsedFrame.data["volume"] as? Double
                        if (volume == null) {
                            logger.error("Invalid volume in websocket message.")
                        }
                        else {
                            ControlRepo.setVolume(sessionId, volume.toFloat())
                        }
                    }
                    "speed" -> {
                        val speed = parsedFrame.data["speed"] as? Double
                        if (speed == null) {
                            logger.error("Invalid speed in websocket message.")
                        }
                        else {
                            ControlRepo.setSpeed(sessionId, speed.toFloat())
                        }
                    }
                    "queue" -> {
                        val queue = parsedFrame.data["queue"] as? List<*>
                        if (queue == null) {
                            logger.error("Invalid queue in websocket message.")
                        }
                        else {
                            try {
                                ControlRepo.setQueue(sessionId, queue.map { item ->
                                    val map = item as? Map<*, *>
                                        ?: throw InvalidBodyException("Queue Item is not a map.")

                                    val id = map["id"] as? String
                                        ?: throw InvalidBodyException("Queue Item ID is not a String.")

                                    val pos = map["pos"] as? Double
                                        ?: throw InvalidBodyException("Queue Item position is not a number.")

                                    val trackId = map["trackId"] as Double?

                                    return@map ControlSessionQueueItemInDto(id, pos.toInt(), trackId?.toLong())
                                })
                            }
                            catch (e: Exception) {
                                logger.error("Error while updating queue in websocket:", e)
                            }
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
            ControlRepo.setState(sessionId, SessionPlayState.STOPPED)
            ControlRepo.remove(sessionId)
            logger.info("WebSocket connection closed")
        }

    }
}