package de.mickkc.vibin.db.playlists

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import de.mickkc.vibin.db.ModifiableLongIdEntity
import de.mickkc.vibin.db.ModifiableLongIdTable
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.UserTable
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.images.ImageTable

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
 * @property collaborators The users who can collaborate on the playlist.
 */
class PlaylistEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, PlaylistTable) {
    companion object : LongEntityClass<PlaylistEntity>(PlaylistTable)

    var name by PlaylistTable.name
    var description by PlaylistTable.description
    var cover by ImageEntity optionalReferencedOn PlaylistTable.cover
    var public by PlaylistTable.public
    var vibeDef by PlaylistTable.vibeDef

    var collaborators by UserEntity.Companion via PlaylistCollaborator
    var owner by UserEntity referencedOn PlaylistTable.owner

    override fun delete() {

        val playlistId = this.id.value

        // Remove associated playlist collaborators
        PlaylistCollaborator.deleteWhere { PlaylistCollaborator.playlist eq playlistId }

        // Remove associated playlist tracks
        PlaylistTrackEntity.find { PlaylistTrackTable.playlistId eq playlistId }.forEach { it.delete() }

        super.delete()
    }
}