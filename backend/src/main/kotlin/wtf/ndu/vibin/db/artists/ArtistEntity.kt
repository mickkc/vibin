package wtf.ndu.vibin.db.artists

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import wtf.ndu.vibin.db.tags.ArtistTagConnection
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable
import wtf.ndu.vibin.db.tags.TagEntity

object ArtistTable : ModifiableLongIdTable("artist") {
    val name = varchar("name", 255).uniqueIndex()
    val originalName = varchar("original_name", 255).nullable()
    val image = reference("image_url", ImageTable).nullable()
    val sortName = varchar("sort_name", 255).nullable().index()
}

/**
 * Artist entity representing a musical artist.
 *
 * @property name The name of the artist.
 * @property originalName The original name of the artist, before any modifications. (optional)
 * @property image The image associated with the artist. (optional)
 * @property sortName The sort name of the artist. (optional)
 * @property tags The tags associated with the artist.
 */
class ArtistEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, ArtistTable) {
    companion object : LongEntityClass<ArtistEntity>(ArtistTable)

    var name by ArtistTable.name
    var originalName by ArtistTable.originalName
    var image by ImageEntity.Companion optionalReferencedOn ArtistTable.image
    var sortName by ArtistTable.sortName
    var tags by TagEntity.Companion via ArtistTagConnection
}