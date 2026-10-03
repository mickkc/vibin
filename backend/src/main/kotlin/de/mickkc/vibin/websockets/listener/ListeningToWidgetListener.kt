package de.mickkc.vibin.websockets.listener

import com.google.gson.Gson
import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.routes.WsFrame
import de.mickkc.vibin.utils.DateTimeUtils.now
import de.mickkc.vibin.websockets.Session
import de.mickkc.vibin.websockets.SessionListener
import de.mickkc.vibin.widgets.WidgetUtils
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class ListeningToWidgetListener(
    session: Session,
    val userId: Long,
    val outgoing: SendChannel<Frame>,
    val external: Boolean
) : SessionListener(session) {

    val logger: Logger = LoggerFactory.getLogger(ListeningToWidgetListener::class.java)
    private val gson = Gson().newBuilder().serializeNulls().create()
    private var stopped = false
    private var reconnecting = false
    private var lastTrackId: Long? = null


    private suspend fun send(frame: WsFrame) {
        try {
            outgoing.send(Frame.Text(gson.toJson(frame)))
        }
        catch (e: Exception) {
            logger.error("Error send event to widget websocket.", e)
        }
    }

    private val allowedTypes = listOf(
        "play",
        "pause",
        "stop",
        "started_track"
    )

    private suspend fun updateState() {

        val track = session.currentTrackId?.let { TrackRepo.getById(it) }
        val album = track?.let { TrackRepo.getAlbum(it) }
        val artists = track?.let { TrackRepo.getArtists(it) }

        val imageUrl = if (lastTrackId != session.currentTrackId) {
            lastTrackId = session.currentTrackId
            val img = track?.let { TrackRepo.getCover(track) }
            WidgetUtils.getImageUrl(img, "track", 128, external)
        }
        else null

        send(WsFrame(
            type = "state",
            data = mapOf(
                "title" to track?.title.orEmpty(),
                "artists" to artists.orEmpty().joinToString { it.name },
                "album" to album?.title.orEmpty(),
                "cover" to imageUrl,
                "state" to session.state.name,
                "updated" to session.lastUpdate,
                "old" to session.removed,
                "duration" to track?.duration,
                "position" to session.lastPosition + (now() - session.lastPositionUpdate)
            ),
            timestamp = now()
        ))
    }

    override suspend fun onFrame(frame: WsFrame) {
        if (frame.type in allowedTypes) {
            updateState()
        }
    }

    suspend fun reconnect() = withContext(Dispatchers.IO) {
        if (reconnecting) return@withContext
        reconnecting = true

        try {
            unregister()

            var currentSession: Session? = null
            while (currentSession == null) {
                if (stopped) break
                currentSession = ControlRepo.getCurrentSession(userId)
                if (currentSession == null) {
                    delay(5.seconds)
                    continue
                }
                else {
                    session = currentSession
                    session.registerListener(this@ListeningToWidgetListener)
                    updateState()
                    break
                }
            }
        }
        catch (e: Exception) {
            logger.error("Error reconnecting to a session for user $userId.", e)
        }
        finally {
            reconnecting = false
        }

    }

    override suspend fun onUnregister() {
        updateState()
        reconnect()
    }

    suspend fun stop() {
        stopped = true
        unregister()
    }
}