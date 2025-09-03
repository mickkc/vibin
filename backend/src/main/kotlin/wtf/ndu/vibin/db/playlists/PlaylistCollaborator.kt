package wtf.ndu.vibin.db.playlists

import org.jetbrains.exposed.sql.Table
import wtf.ndu.vibin.db.UserTable

/**
 * Connection table between playlists and their collaborators (users).
 *
 * @property playlist Reference to the playlist.
 * @property user Reference to the user (collaborator).
 * @primaryKey Composite primary key consisting of playlist and user.
 */
object PlaylistCollaborator : Table() {
    val playlist = reference("playlist_id", PlaylistTable)
    val user = reference("user_id", UserTable)

    override val primaryKey = PrimaryKey(playlist, user, name = "PK_PlaylistCollaborator_playlist_user")
}