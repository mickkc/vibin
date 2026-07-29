package de.mickkc.vibin.websockets

import com.google.gson.Gson
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.ControlSessionQueueItemInDto
import de.mickkc.vibin.dto.ControlSessionQueueItemOutDto
import de.mickkc.vibin.repos.*
import de.mickkc.vibin.routes.WsFrame
import de.mickkc.vibin.utils.DateTimeUtils.now
import io.ktor.server.application.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

@Suppress("FunctionName") // set*() methods interfer with kotlins setter generation for these values
class Session(
    val userId: Long,
    val sessionId: String,
    val outgoing: SendChannel<Frame>,
    var state: SessionPlayState = SessionPlayState.UNKNOWN,
    var currentTrackId: Long? = null,
    var queue: List<ControlSessionQueueItemOutDto> = emptyList(),
    var repeatState: SessionRepeatState = SessionRepeatState.OFF,
    var shuffleState: Boolean = false,
    var volume: Float = 1f,
    var speed: Float = 1f,
    @Volatile var lastPosition: Long = 0L,
    @Volatile var lastPositionUpdate: Long = now(),
    var lastUpdate: Long = now(),
    val lock: Lock = ReentrantLock(),
    var removed: Boolean = false,
) {
    private val listeners: MutableList<SessionListener> = mutableListOf()

    private val outgoingMessageFlow = MutableSharedFlow<WsFrame>()

    /**
     * A message flow that can be listened to to get updates on this session's state.
     */
    val outgoingFlow = outgoingMessageFlow.asSharedFlow()

    val logger: Logger = LoggerFactory.getLogger("Websocket-Playback-User-$userId-$sessionId")

    private val gson = Gson()

    fun registerListener(listener: SessionListener) {
        listeners.add(listener)
    }

    suspend fun unregisterListener(listener: SessionListener) {
        listener.onUnregister()
        listeners.remove(listener)
    }

    /**
     * Send data to the client.
     *
     * @param type The message type.
     * @param data Additional data.
     * @return true if the data was sucessfully sent, false if an error occurred.
     */
    private suspend fun sendData(type: String, data: Map<String, @Serializable @Contextual Any?>): Boolean = withContext(Dispatchers.IO) {
        try {
            outgoing.send(Frame.Text(gson.toJson(WsFrame(type, data, now()))))
            return@withContext true
        }
        catch (e: Exception) {
            logger.error("Error sending event to websocket.", e)
            return@withContext false
        }
    }

    suspend fun sendState(state: SessionPlayState) = sendData("state", mapOf("state" to state.name))
    suspend fun sendCurrentQueueTrackId(pos: Int) = sendData("currentTrack", mapOf("queueId" to pos))
    suspend fun sendCurrentTrackId(id: Long) = sendData("currentTrack", mapOf("id" to id))
    suspend fun sendQueue(request: SetQueueRequest) = sendData("queue", mapOf(
        "ids" to request.ids,
        "startId" to request.startId,
        "audioType" to request.audioType,
        "audioTypeId" to request.audioTypeId,
        "shuffle" to request.shuffle
    ))
    suspend fun sendShuffleState(shuffle: Boolean) = sendData("shuffle", mapOf("state" to shuffle))
    suspend fun sendRepeatState(repeatState: SessionRepeatState) = sendData("repeat", mapOf("state" to repeatState.name))
    suspend fun sendVolume(volume: Float) = sendData("volume", mapOf("volume" to volume))
    suspend fun sendSpeed(speed: Float) = sendData("speed", mapOf("speed" to speed))

    private fun _setState(state: SessionPlayState) = apply { this.state = state }
    private fun _setCurrentTrackId(id: Long) = apply { this.currentTrackId = id }
    private fun _setRepeatState(state: SessionRepeatState) = apply { this.repeatState = state }
    private fun _setShuffleState(state: Boolean) = apply { this.shuffleState = state }
    private fun _setVolume(volume: Float) = apply { this.volume = volume }
    private fun _setSpeed(speed: Float) = apply { this.speed = speed }
    private fun _setQueue(queue: List<ControlSessionQueueItemInDto>) {
        val outQueue = ControlRepo.queueInToOut(queue)
        apply { this.queue = outQueue }
    }

    /**
     * Wait for the session to be available, the run the block.
     *
     * If the session has been removed while waiting, the action is not performed and null is returned.
     *
     * @param block The code to execute once the session is available.
     * @return The session, or null if the session has been removed while waiting.
     */
    fun apply(block: (Session) -> Unit): Session? {
        var result: Session? = null
        try {
            lock.lock()
            if (!removed) {
                block(this)
                lastUpdate = now()
                result = this
            }
        }
        finally {
            lock.unlock()
        }
        return result
    }

    suspend fun applyToListeners(block: suspend SessionListener.() -> Unit) {
        listeners.forEach { it.block() }
    }

    private fun tryUpdatePosition(frame: WsFrame) {
        val position = (frame.data["position"] as? Double)?.toLong()
        if (position != null) {
            lastPosition = position
            lastPositionUpdate = now()
        }
    }

    /**
     * Handles an incomming websocket frame.
     *
     * This updates both the sessions state and the listening tracking.
     *
     * @param frame The incomming frame.
     */
    suspend fun handle(frame: WsFrame) {
        outgoingMessageFlow.emit(frame)

        when (frame.type) {
            "ping" -> {
                applyToListeners { onPing(frame.timestamp) }
            }
            "finished_track" -> {
                val trackId = (frame.data["trackId"] as? Double)?.toLong()
                applyToListeners { onTrackFinished(trackId, frame.timestamp) }
            }
            "play" -> {
                tryUpdatePosition(frame)
                _setState(SessionPlayState.PLAYING)
                applyToListeners { onPlay(frame.timestamp) }
            }
            "pause" -> {
                tryUpdatePosition(frame)
                _setState(SessionPlayState.PAUSED)
                applyToListeners { onPause(frame.timestamp) }
            }
            "stop" -> {
                tryUpdatePosition(frame)
                _setState(SessionPlayState.STOPPED)
                applyToListeners { onStop(frame.timestamp) }
            }
            "buffering" -> {
                _setState(SessionPlayState.BUFFERING)
                applyToListeners { onBuffering(frame.timestamp) }
            }
            "skipped_next" -> {
                val trackId = (frame.data["trackId"] as? Double)?.toLong()
                if (trackId != null) {
                    applyToListeners { onTrackSkippedNext(trackId, frame.timestamp) }
                }
            }
            "skipped_prev" -> {
                val trackId = (frame.data["trackId"] as? Double)?.toLong()
                if (trackId != null) {
                    applyToListeners { onTrackSkippedPrev(trackId, frame.timestamp) }
                }
            }
            "started_track" -> {
                val trackId = (frame.data["trackId"] as? Double)?.toLong()
                if (trackId != null) {
                    _setCurrentTrackId(trackId)
                    applyToListeners { onTrackStarted(trackId, frame.timestamp) }
                }
            }
            "listen" -> {
                val type = frame.data["type"] as? String
                val id = (frame.data["id"] as? Double)?.toLong() ?: return

                val parsedType = when (type) {
                    "track" -> ListenType.TRACK
                    "album" -> ListenType.ALBUM
                    "playlist" -> ListenType.PLAYLIST
                    "artist" -> ListenType.ARTIST
                    else -> {
                        logger.error("Unknown listen type: $type")
                        return
                    }
                }

                applyToListeners { onListen(parsedType, id, frame.timestamp) }
            }
            "repeat" -> {
                val parsedMode = when (val mode = frame.data["mode"] as? String) {
                    "all" -> SessionRepeatState.ALL
                    "one" -> SessionRepeatState.CURRENT_TRACK
                    "off" -> SessionRepeatState.OFF
                    else -> {
                        logger.warn("Invalid repeat mode: $mode")
                        return
                    }
                }

                _setRepeatState(parsedMode)
                applyToListeners { onRepeatChanged(parsedMode, frame.timestamp) }
            }
            "shuffle" -> {
                val mode = frame.data["shuffle"] as? Boolean
                if (mode == null) {
                    logger.error("Invalid shuffle mode in websocket message.")
                    return
                }

                _setShuffleState(mode)
                applyToListeners { onShuffleChanged(mode, frame.timestamp) }
            }
            "volume" -> {
                val volume = frame.data["volume"] as? Double
                if (volume == null) {
                    logger.error("Invalid volume in websocket message.")
                    return
                }

                _setVolume(volume.toFloat())
                applyToListeners { onVolumeChanged(volume, frame.timestamp) }
            }
            "speed" -> {
                val speed = frame.data["speed"] as? Double
                if (speed == null) {
                    logger.error("Invalid speed in websocket message.")
                    return
                }

                _setSpeed(speed.toFloat())
                applyToListeners { onSpeedChanged(speed, frame.timestamp) }
            }
            "queue" -> {
                val queue = frame.data["queue"] as? List<*>
                if (queue == null) {
                    logger.error("Invalid queue in websocket message.")
                    return
                }

                try {
                    val queue = queue.map { item ->
                        val map = item as? Map<*, *>
                            ?: throw InvalidBodyException("Queue Item is not a map.")

                        val id = map["id"] as? String
                            ?: throw InvalidBodyException("Queue Item ID is not a String.")

                        val pos = map["pos"] as? Double
                            ?: throw InvalidBodyException("Queue Item position is not a number.")

                        val trackId = map["trackId"] as Double?

                        return@map ControlSessionQueueItemInDto(id, pos.toInt(), trackId?.toLong())
                    }
                    _setQueue(queue)
                    applyToListeners { onQueueChanged(queue, frame.timestamp) }
                }
                catch (e: Exception) {
                    logger.error("Error while updating queue in websocket:", e)
                }
            }
            else -> {
                logger.warn("Unknown WebSocket frame type: ${frame.type}")
            }
        }

        applyToListeners { onFrame(frame) }
    }

    /**
     * Closes this connection.
     *
     * Notifies clients listening to this session to close too.
     * Notifies the listening tracking repo that playback has been stopped and deletes the sessions' data.
     */
    suspend fun close() {
        logger.info("WebSocket connection closed")
        _setState(SessionPlayState.STOPPED)
        outgoingMessageFlow.emit(WsFrame("close", emptyMap(), now()))
        applyToListeners {
            onUnregister()
            onClose()
        }
        listeners.clear()
    }
}
