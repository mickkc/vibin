package wtf.ndu.vibin.db.albums

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable

object AlbumTable : ModifiableLongIdTable("album") {
    val title = varchar("title", 255)
    val releaseYear = integer("release_year").nullable()
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
    var releaseYear by AlbumTable.releaseYear
    var cover by ImageEntity.Companion optionalReferencedOn AlbumTable.cover
}