package wtf.ndu.vibin.db.playlists

import org.jetbrains.exposed.sql.Table
import wtf.ndu.vibin.db.tracks.TrackTable

/**
 * Connection table between playlists and tracks.
 *
 * @property playlist Reference to the playlist.
 * @property track Reference to the track.
 * @primaryKey Composite primary key consisting of playlist and track.
 */
object PlaylistTrackTable : Table("playlist_track") {
    val playlist = reference("playlist_id", PlaylistTable)
    val track = reference("track_id", TrackTable)
    val position = integer("position").default(0)

    override val primaryKey = PrimaryKey(playlist, track, name = "PK_PlaylistTrack_playlist_track")
}