package de.mickkc.vibin.repos

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.db.TrackListenDurationEntity
import de.mickkc.vibin.db.TrackListenDurationTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.utils.DateTimeUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Sum
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.round

object ListenTrackingRepo {

    private data class Listen(
        var trackId: Long,
        var userId: Long,
        var listenStartTimestamp: Long?,
        var totalListenedSeconds: Long = 0L,
        val created: Long = DateTimeUtils.now()
    ) {
        fun start(startTimestamp: Long) {
            if (listenStartTimestamp != null) return
            listenStartTimestamp = startTimestamp
        }

        fun stop(stopTimestamp: Long) {
            listenStartTimestamp?.let {
                val listenedSeconds = stopTimestamp - it
                totalListenedSeconds += listenedSeconds
                listenStartTimestamp = null
            }
        }
    }

    private val listenMap = ConcurrentHashMap<String, Listen>()
    private val offsetsMap = ConcurrentHashMap<String, Long>()

    /**
     * Called when a track starts playing.
     * If there is an existing listen for the session, it is stopped and recorded before starting the new one.
     *
     * @param sessionId The unique session identifier for the user session.
     * @param userId The ID of the user.
     * @param trackId The ID of the track being played.
     * @param timestamp The timestamp (in seconds) when the track started.
     */
    fun trackStarted(sessionId: String, userId: Long, trackId: Long, timestamp: Long) {
        calculateOffset(sessionId, timestamp)

        listenMap.compute(sessionId) { _, existingListen ->
            existingListen?.let {
                it.stop(timestamp)
                createListenEntity(it)
            }

            Listen(
                trackId = trackId,
                userId = userId,
                listenStartTimestamp = timestamp
            )
        }
    }

    /**
     * Called when a track is paused.
     *
     * @param sessionId The unique session identifier for the user session.
     * @param timestamp The timestamp (in seconds) when the track was paused.
     */
    fun trackPaused(sessionId: String, timestamp: Long) {
        calculateOffset(sessionId, timestamp)

        listenMap.computeIfPresent(sessionId) { _, listen ->
            listen.stop(timestamp)
            listen
        }
    }

    /**
     * Called when a track is resumed after being paused.
     *
     * @param sessionId The unique session identifier for the user session.
     * @param timestamp The timestamp (in seconds) when the track was resumed.
     */
    fun trackResumed(sessionId: String, timestamp: Long) {
        calculateOffset(sessionId, timestamp)

        listenMap.computeIfPresent(sessionId) { _, listen ->
            listen.start(timestamp)
            listen
        }
    }

    /**
     * Called when a track is skipped.
     *
     * @param sessionId The unique session identifier for the user session.
     * @param timestamp The timestamp (in seconds) when the track was skipped.
     */
    fun trackSkipped(sessionId: String, timestamp: Long) {
        calculateOffset(sessionId, timestamp)

        listenMap.computeIfPresent(sessionId) { _, listen ->
            listen.stop(timestamp)
            createListenEntity(listen)
            null // Remove the entry
        }
    }

    /**
     * Called when a track finishes playing.
     *
     * @param sessionId The unique session identifier for the user session.
     * @param timestamp The timestamp (in seconds) when the track finished.
     */
    fun trackFinished(sessionId: String, trackId: Long, timestamp: Long) {
        calculateOffset(sessionId, timestamp)

        listenMap.computeIfPresent(sessionId) { _, listen ->
            if (listen.trackId == trackId) {
                listen.stop(timestamp)
                createListenEntity(listen)
                null // Remove the entry
            } else {
                listen // Wrong track, do nothing
            }
        }
    }

    /**
     * Called when the listening session is stopped.
     *
     * @param sessionId The unique session identifier for the user session.
     * @param timestamp The timestamp (in seconds) when the session was stopped.
     */
    fun stopped(sessionId: String, timestamp: Long) {
        calculateOffset(sessionId, timestamp)

        listenMap.computeIfPresent(sessionId) { _, listen ->
            listen.stop(timestamp)
            createListenEntity(listen)
            null // Remove the entry
        }
    }

    private fun createListenEntity(listen: Listen) = transaction {
        val user = UserEntity.findById(listen.userId) ?: return@transaction
        val track = TrackEntity.findById(listen.trackId) ?: return@transaction

        if (track.duration == null) return@transaction

        // Ensure listened duration does not exceed track duration
        val normalizedDuration = listen.totalListenedSeconds.coerceIn(0L, round(track.duration!! / 1000.0).toLong())

        TrackListenDurationEntity.new {
            this.user = user
            this.track = track
            this.listenedDurationSeconds = normalizedDuration
            this.createdAt = listen.created
        }

        ListenRepo.listenedTo(
            userId = listen.userId,
            entityId = listen.trackId,
            type = ListenType.TRACK,
            at = listen.created
        )
    }

    private fun calculateOffset(sessionId: String, timestamp: Long): Long {
        val now = DateTimeUtils.now()
        val offset = now - timestamp
        offsetsMap[sessionId] = offset
        return offset
    }

    fun getNowWithOffset(sessionId: String): Long {
        val offset = offsetsMap.getOrDefault(sessionId, 0L)
        return DateTimeUtils.now() - offset
    }

    fun deleteSessionData(sessionId: String) {
        listenMap.remove(sessionId)
        offsetsMap.remove(sessionId)
    }

    fun getGlobalListenDuration() = transaction {
        val rs = TrackListenDurationTable.select(Sum(
            expr = TrackListenDurationTable.listenedDurationSeconds,
            columnType = TrackListenDurationTable.listenedDurationSeconds.columnType))
            .execute(this)

        if (rs != null && rs.next()) {
            return@transaction rs.getLong(1)
        }

        return@transaction 0L
    }

    fun getGlobalListenDurationForTrack(trackId: Long): Long = transaction {
        val rs = TrackListenDurationTable
            .select(Sum(
                expr = TrackListenDurationTable.listenedDurationSeconds,
                columnType = TrackListenDurationTable.listenedDurationSeconds.columnType))
            .where { TrackListenDurationTable.trackId eq trackId }
            .execute(this)

        if (rs != null && rs.next()) {
            return@transaction rs.getLong(1)
        }

        return@transaction 0L
    }

    fun getUserListenDurationForTrack(trackId: Long, userId: Long): Long = transaction {
        val rs = TrackListenDurationTable
            .select(Sum(
                expr = TrackListenDurationTable.listenedDurationSeconds,
                columnType = TrackListenDurationTable.listenedDurationSeconds.columnType))
            .where { (TrackListenDurationTable.userId eq userId) and (TrackListenDurationTable.trackId eq trackId) }
            .execute(this)

        if (rs != null && rs.next()) {
            return@transaction rs.getLong(1)
        }

        return@transaction 0L
    }

    /**
     * Removes a track from all `TrackListenDurationTable` rows by setting `trackId` to null.
     *
     * They keep getting stored in the database, because they still count as something users have listened to in the
     * past.
     *
     * @param trackId The track id to remove from all tracked listening durations
     */
    fun trackDeleted(trackId: Long) = transaction {
        TrackListenDurationTable
            .update(
                where = { TrackListenDurationTable.trackId eq trackId }
            ) { statement: UpdateStatement ->
                statement[TrackListenDurationTable.trackId] = null
            }
    }

    /**
     * Removes all listening duration from a track.
     *
     * Only do this if specifically requested by the user.
     * If a track is deleted, use @see trackDeleted instead!
     *
     * @param trackId The track id to remove all durations for.
     */
    fun deleteAllForTrack(trackId: Long) = transaction {
        TrackListenDurationTable
            .deleteWhere { TrackListenDurationTable.trackId eq trackId }
    }
}