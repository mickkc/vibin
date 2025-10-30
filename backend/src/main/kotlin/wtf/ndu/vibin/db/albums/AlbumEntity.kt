package wtf.ndu.vibin.db.albums

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.db.tracks.TrackTable

object AlbumTable : ModifiableLongIdTable("album") {
    val title = varchar("title", 255).index()
    val description = text("description").default("")
    val releaseYear = integer("release_year").nullable()
    val single = bool("single").nullable()
    val cover = reference("cover", ImageTable).nullable()
}

/**
 * Album entity representing a music album.
 *
 * @property title The title of the album.
 * @property releaseYear The release year of the album, nullable.
 * @property cover The cover image of the album, nullable.
 */
class AlbumEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, AlbumTable) {
    companion object : LongEntityClass<AlbumEntity>(AlbumTable)

    var title by AlbumTable.title
    var description by AlbumTable.description
    var releaseYear by AlbumTable.releaseYear
    var single by AlbumTable.single
    var cover by ImageEntity.Companion optionalReferencedOn AlbumTable.cover

    override fun delete() {

        val albumId = this.id.value

        // Delete tracks associated with this album
        TrackEntity.find { TrackTable.albumId eq albumId }.forEach { it.delete() }

        super.delete()
    }
}