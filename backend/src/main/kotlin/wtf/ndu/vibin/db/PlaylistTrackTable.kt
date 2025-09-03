package wtf.ndu.vibin.db

import org.jetbrains.exposed.sql.Table

object PlaylistTrackTable : Table() {
    val playlist = reference("playlist_id", PlaylistTable)
    val track = reference("track_id", TrackTable)

    override val primaryKey = PrimaryKey(playlist, track, name = "PK_PlaylistTrack_playlist_track")
}