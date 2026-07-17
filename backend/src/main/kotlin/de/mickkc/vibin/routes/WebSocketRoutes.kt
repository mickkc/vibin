package de.mickkc.vibin.routes

import com.google.gson.Gson
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.SessionRepo
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

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
        val session = ControlRepo.add(userId, sessionId, outgoing)

        session.logger.info("WebSocket connection established: ${this.call.request.local.remoteHost}")

        try {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                val receivedText = frame.readText()
                session.logger.info("Received via WebSocket: $receivedText")

                val parsedFrame = parser.fromJson(receivedText, WsFrame::class.java)
                session.handle(parsedFrame)
            }
        }
        catch (e: Exception) {
            session.logger.error("WebSocket error: ${e.localizedMessage}", e)
        }
        finally {
            session.close()
            ControlRepo.remove(sessionId)
        }
    }
}