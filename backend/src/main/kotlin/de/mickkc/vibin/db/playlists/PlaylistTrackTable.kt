package de.mickkc.vibin.db.playlists

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.utils.DateTimeUtils

/**
 * Connection table between playlists and tracks.
 *
 * @property playlistId Reference to the playlist.
 * @property trackId Reference to the track.
 * @property position Position of the track in the playlist.
 * @property userId Reference to the user who added the track.
 * @property addedAt Timestamp when the track was added to the playlist.
 * @primaryKey Composite primary key consisting of playlist and track.
 */
object PlaylistTrackTable : LongIdTable("playlist_track") {
    val playlistId = reference("playlist_id", PlaylistTable)
    val trackId = reference("track_id", TrackTable)
    val position = integer("position").default(0)
    val userId = reference("user_id", UserTable)
    val addedAt = long("added_at").clientDefault { DateTimeUtils.now() }
}

class PlaylistTrackEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<PlaylistTrackEntity>(PlaylistTrackTable)

    var playlist by PlaylistEntity referencedOn PlaylistTrackTable.playlistId
    var track by TrackEntity referencedOn PlaylistTrackTable.trackId
    var position by PlaylistTrackTable.position
    var user by UserEntity referencedOn PlaylistTrackTable.userId
    var addedAt by PlaylistTrackTable.addedAt
}