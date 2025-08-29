package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.ArtistEntity
import wtf.ndu.vibin.db.ArtistTable

object ArtistRepo {

    /**
     * Retrieves an existing artist by name or creates a new one if it doesn't exist.
     *
     * @param name The name of the artist to retrieve or create.
     * @return The existing or newly created ArtistEntity.
     */
    fun getOrCreateArtist(name: String): ArtistEntity = transaction {
        return@transaction ArtistEntity.find { ArtistTable.name.lowerCase() eq name.lowercase() }.firstOrNull()
            ?: ArtistEntity.new { this.name = name }
    }

}