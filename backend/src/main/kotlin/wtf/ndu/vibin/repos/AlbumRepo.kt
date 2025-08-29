package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.AlbumEntity
import wtf.ndu.vibin.db.AlbumTable

object AlbumRepo {

    private val logger = LoggerFactory.getLogger(AlbumRepo::class.java)

    /**
     * Retrieves an existing album by title or creates a new one if it doesn't exist.
     *
     * @param title The title of the album to retrieve or create.
     * @return The existing or newly created AlbumEntity.
     */
    fun getOrCreateAlbum(title: String): AlbumEntity = transaction {
        return@transaction AlbumEntity.find { AlbumTable.title.lowerCase() eq title.lowercase() }.firstOrNull()
            ?: AlbumEntity.new { this.title = title }
    }
}