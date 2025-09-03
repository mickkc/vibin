package wtf.ndu.vibin.db

import org.jetbrains.exposed.sql.Table

object PlaylistCollaborator : Table() {
    val playlist = reference("playlist_id", PlaylistTable)
    val user = reference("user_id", UserTable)

    override val primaryKey = PrimaryKey(playlist, user, name = "PK_PlaylistCollaborator_playlist_user")
}