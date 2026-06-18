package de.mickkc.vibin.repos

import de.mickkc.vibin.utils.DateTimeUtils.now
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * @property sessionId The session id - should be unique for each session.
 * @property userId The id of the user this session belongs to.
 * @property playing Playing status; true if playing, false if not.
 * @property currentTrackId The id of the current track, or null if there is none.
 * @property queue A list of track ids that are currently in the queue.
 *                 This includes tracks before the current track as well as the current track itself.
 * @property lastUpdate The timestamp the session was last updated.
 * @property lock The lock used to avoid race conditions while modifying.
 * @property removed Because multiple actions could be performed on one session at once, the session could be removed,
 *                   and then be updated. To prevent this, all updates fail if removed is true.
 */
data class ControlSession(
    val sessionId: String,
    val userId: Long,
    var playing: Boolean,
    var currentTrackId: Long?,
    var queue: List<Long>,
    var lastUpdate: Long = now(),
    val lock: Lock = ReentrantLock(),
    var removed: Boolean = false
)

object ControlRepo {

    private val sessions = ConcurrentHashMap<String, ControlSession>()
    private val logger = LoggerFactory.getLogger(ControlRepo::class.java)

    /**
     * Locks the sessions lock, executes `block` on it, and releases the lock.
     *
     * @param session The session to modify
     * @param block The code to update the session
     */
    private fun lock(session: ControlSession, block: (ControlSession) -> Unit) {
        try {
            session.lock.lock()
            block(session)
        }
        catch (e: Exception) {
            logger.error("An error occured while processing session ${session.sessionId}!", e)
        }
        finally {
            session.lock.unlock()
        }
    }

    /**
     * Adds a new session, optionally initializing it using `init`.
     *
     * @param sessionId The id for the new session
     * @param userId The id of the user the session is for
     * @param init Optional initialization code, executed thread-safely (Is this even a word?)
     * @return The created session object
     */
    fun add(sessionId: String, userId: Long, init: ((ControlSession) -> Unit)? = null): ControlSession {
        var isNew = false
        val session = sessions.computeIfAbsent(sessionId) {
            isNew = true
            ControlSession(sessionId, userId, false, null, emptyList())
        }
        if (isNew) {
            lock(session) { init?.invoke(session) }
        }
        return session
    }

    /**
     * Safely removes a session from the list.
     *
     * @param sessionId The id of the session to remove.
     * @return The session that was removed (null if no session was removed).
     */
    fun remove(sessionId: String): ControlSession? {
        val session = sessions.remove(sessionId) ?: return null
        lock(session) { session.removed = true }
        return session
    }

    /**
     * Gets a session by its id.
     *
     * @param id The session id
     * @return The session, or null if not found.
     */
    fun getById(id: String): ControlSession? {
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
    fun getAllForUser(userId: Long): List<ControlSession> {
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
    fun getCurrentSession(userId: Long): ControlSession? {
        return getAllForUser(userId).maxByOrNull { it.lastUpdate }
    }

    /**
     * Applies the `block` to the session, if a session with the specified `sessionId` is found.
     *
     * @param sessionId The id of the session to modify
     * @param block The changes applied to the session (if found)
     * @return The modified session, or null if no session with `sessionId` was found.
     */
    private fun apply(sessionId: String, block: (ControlSession) -> Unit): ControlSession? {
        val session = getById(sessionId) ?: return null
        var result: ControlSession? = null
        lock(session) {
            if (!session.removed) {
                block(session)
                session.lastUpdate = now()
                result = session
            }
        }
        return result
    }

    fun setIsPlaying(sessionId: String, isPlaying: Boolean): ControlSession? = apply(sessionId) {
        it.playing = isPlaying
    }

    fun setIsCurrentTrackId(sessionId: String, trackId: Long?): ControlSession? = apply(sessionId) {
        it.currentTrackId = trackId
    }

    fun setQueue(sessionId: String, queue: List<Long>): ControlSession? = apply(sessionId) {
        it.queue = queue
    }
}