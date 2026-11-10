package de.mickkc.vibin.db.artists

import de.mickkc.vibin.db.ModifiableLongIdEntity
import de.mickkc.vibin.db.ModifiableLongIdTable
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.images.ImageTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.jdbc.deleteWhere

object ArtistTable : ModifiableLongIdTable("artist") {
    val name = varchar("name", 255).index()
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