package wtf.ndu.vibin.db.playlists

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.UserTable
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable

object PlaylistTable : ModifiableLongIdTable("playlist") {
    val name = varchar("name", 255)
    val description = text("description").default("")
    val cover = reference("cover_id", ImageTable).nullable()
    val public = bool("public").default(false)
    val vibeDef = text("vibe_def").nullable()
    val owner = reference("owner_id", UserTable)
}

/**
 * Playlist entity representing a music playlist.
 *
 * @property name The name of the playlist.
 * @property description The description of the playlist.
 * @property cover The cover image of the playlist. (optional)
 * @property public Whether the playlist is public or private.
 * @property vibeDef The vibe definition of the playlist. (optional)
 * @property tracks The tracks in the playlist.
 * @property collaborators The users who can collaborate on the playlist.
 */
class PlaylistEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, PlaylistTable) {
    companion object : LongEntityClass<PlaylistEntity>(PlaylistTable)

    var name by PlaylistTable.name
    var description by PlaylistTable.description
    var cover by ImageEntity optionalReferencedOn PlaylistTable.cover
    var public by PlaylistTable.public
    var vibeDef by PlaylistTable.vibeDef

    var tracks by TrackEntity.Companion via PlaylistTrackTable orderBy PlaylistTrackTable.position
    var collaborators by UserEntity.Companion via PlaylistCollaborator
    var owner by UserEntity referencedOn PlaylistTable.owner
}