package wtf.ndu.vibin.db

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object ArtistTable : ModifiableLongIdTable("artist") {
    val name = varchar("name", 255).uniqueIndex()
    val image = reference("image_url", ImageTable).nullable()
    val sortName = varchar("sort_name", 255).nullable().index()
}

/**
 * Artist entity representing a musical artist.
 *
 * @property name The name of the artist.
 * @property image The image associated with the artist. (optional)
 * @property sortName The sort name of the artist. (optional)
 * @property tags The tags associated with the artist.
 */
class ArtistEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, ArtistTable) {
    companion object : LongEntityClass<ArtistEntity>(ArtistTable)

    var name by ArtistTable.name
    var image by ImageEntity optionalReferencedOn ArtistTable.image
    var sortName by ArtistTable.sortName
    var tags by TagEntity via ArtistTagConnection
}