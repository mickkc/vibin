package wtf.ndu.vibin.db.artists

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import wtf.ndu.vibin.db.ModifiableLongIdEntity
import wtf.ndu.vibin.db.ModifiableLongIdTable
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.images.ImageTable

object ArtistTable : ModifiableLongIdTable("artist") {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").default("")
    val image = reference("image_url", ImageTable).nullable()
}

/**
 * Artist entity representing a musical artist.
 *
 * @property name The name of the artist.
 * @property description A description of the artist.
 * @property image The image associated with the artist. (optional)
 */
class ArtistEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, ArtistTable) {
    companion object : LongEntityClass<ArtistEntity>(ArtistTable)

    var name by ArtistTable.name
    var description by ArtistTable.description
    var image by ImageEntity.Companion optionalReferencedOn ArtistTable.image

    override fun delete() {

        val artistId = this.id.value

        // Delete any connections to tracks
        TrackArtistConnection.deleteWhere { TrackArtistConnection.artist eq artistId }

        super.delete()
    }
}