package de.mickkc.vibin.repos

import de.mickkc.vibin.dto.ControlSessionDto
import de.mickkc.vibin.dto.ControlSessionQueueItemInDto
import de.mickkc.vibin.dto.ControlSessionQueueItemOutDto
import de.mickkc.vibin.websockets.Session
import io.ktor.websocket.*
import kotlinx.coroutines.channels.SendChannel
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

enum class SessionPlayState {
    PLAYING,
    PAUSED,
    STOPPED,
    BUFFERING,
    UNKNOWN
}

enum class SessionRepeatState {
    OFF,
    CURRENT_TRACK,
    ALL
}

data class SetQueueRequest(
    val ids: List<Long>,
    val startId: Long?,
    val audioType: String?,
    val audioTypeId: Long?,
    val shuffle: Boolean?
)

object ControlRepo {

    private val sessions = ConcurrentHashMap<String, Session>()
    private val latestSessions = ConcurrentHashMap<Long, Session>()

    /**
     * Adds a new session, optionally initializing it using `init`.
     *
     * @param sessionId The id for the new session
     * @param userId The id of the user the session is for
     * @param init Optional initialization code, executed thread-safely (Is this even a word?)
     * @return The created session object
     */
    fun add(userId: Long, sessionId: String, outgoing: SendChannel<Frame>, init: ((Session) -> Unit)? = null): Session {
        var isNew = false
        val session = sessions.computeIfAbsent(sessionId) {
            isNew = true
            Session(userId, sessionId, outgoing)
        }
        if (isNew) {
            session.apply { init?.invoke(session) }
        }
        return session
    }

    /**
     * Safely removes a session from the list.
     *
     * @param sessionId The id of the session to remove.
     * @return The session that was removed (null if no session was removed).
     */
    fun remove(sessionId: String): Session? {
        val session = sessions.remove(sessionId) ?: return null
        session.apply { session.removed = true }

        val latestSession = latestSessions[session.userId]
        if (latestSession == null || latestSession.lastUpdate < session.lastUpdate) {
            latestSessions[session.userId] = session
        }

        return session
    }

    /**
     * Gets a session by its id.
     *
     * @param id The session id
     * @return The session, or null if not found.
     */
    fun getById(id: String): Session? {
        return sessions[id]
    }

    /**
     * Returns a list of all sessions belonging to the user with the specified user id.
     *
     * Since sessions can asynchronously be added or removed, this is just a snapshot.
     *
     * @param userId The ID of the user to get the sessions for
     * @return All Sessions belonging to the user. If the user does not exist or has no current sessions, the list is empty.
     */
    fun getAllForUser(userId: Long): List<Session> {
        return sessions.values.filter { it.userId == userId }
    }

    /**
     * Returns the current session for the user with the specified user id.
     *
     * The session that was last modified is considered the current session.
     *
     * @param userId The ID of the user to get the current session for
     * @return The users current session. If the user does not exist or has no current sessions, this is null.
     */
    fun getCurrentSession(userId: Long): Session? {
        return getAllForUser(userId).maxByOrNull { it.lastUpdate }
    }

    fun getLatestSession(userId: Long): Session? {
        return latestSessions[userId]
    }

    fun queueInToOut(inList: List<ControlSessionQueueItemInDto>): List<ControlSessionQueueItemOutDto> = transaction {
        return@transaction inList.map { item ->
            ControlSessionQueueItemOutDto(
                id = item.id,
                track = item.trackId?.let { TrackRepo.getById(it) }?.let { TrackRepo.toMinimalDtoInternal(it) }
            )
        }
    }

    fun toDto(session: Session): ControlSessionDto = toDtoInternal(session)
    fun toDto(sessions: Iterable<Session>): List<ControlSessionDto> = sessions.map { toDtoInternal(it) }

    fun toDtoInternal(session: Session): ControlSessionDto = transaction {
        return@transaction ControlSessionDto(
            sessionId = session.sessionId,
            userId = session.userId,
            state = session.state.name,
            currentTrack = session.currentTrackId
                ?.let { TrackRepo.getById(it) }
                ?.let { TrackRepo.toDtoInternal(it) },
            queue = session.queue,
            volume = session.volume,
            speed = session.speed,
            repeat = session.repeatState.name,
            shuffle = session.shuffleState,
            lastUpdate = session.lastUpdate
        )
    }
}