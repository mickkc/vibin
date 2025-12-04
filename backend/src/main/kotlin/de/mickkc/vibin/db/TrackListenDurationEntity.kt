package de.mickkc.vibin.db

import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object TrackListenDurationTable : LongIdTable() {
    val trackId = reference("track_id", TrackTable).nullable()
    val userId = reference("user_id", UserTable)
    val listenedDurationSeconds = long("listened_duration_seconds")
    val createdAt = long("created_at")
}

/**
 * Entity representing a record of the duration a user has listened to a specific track.
 *
 * @property track Reference to the track being listened to (nullable, because times are kept even if the track is deleted).
 * @property user Reference to the user who listened to the track.
 * @property listenedDurationSeconds The total duration in seconds that the user listened to the track.
 * @property createdAt Timestamp (in seconds) when the record was created.
 */
class TrackListenDurationEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<TrackListenDurationEntity>(TrackListenDurationTable)

    var track by TrackEntity optionalReferencedOn TrackListenDurationTable.trackId
    var user by UserEntity referencedOn TrackListenDurationTable.userId
    var listenedDurationSeconds by TrackListenDurationTable.listenedDurationSeconds
    var createdAt by TrackListenDurationTable.createdAt
}