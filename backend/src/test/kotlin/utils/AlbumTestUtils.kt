package utils

import org.junit.jupiter.api.assertNotNull
import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.dto.albums.AlbumEditDto
import de.mickkc.vibin.repos.AlbumRepo
import org.jetbrains.exposed.sql.transactions.transaction

object AlbumTestUtils {

    suspend fun createAlbum(title: String, description: String? = null, year: Int? = null, isSingle: Boolean = false): AlbumEntity {
        val album = AlbumRepo.create(
            AlbumEditDto(
                title = title,
                description = description,
                year = year,
                coverUrl = null,
                isSingle = isSingle
            )
        )
        assertNotNull(album)
        return album
    }

    suspend fun getOrCreateAlbum(title: String): AlbumEntity {
        val existingAlbum = AlbumRepo.getByTitle(title)
        if (existingAlbum != null) {
            return existingAlbum
        }
        return createAlbum(title)
    }

    fun addCoverToAlbum(album: AlbumEntity, coverChecksum: String) = transaction {
        album.cover = ImageEntity.new {
            this.sourceChecksum = coverChecksum
            this.sourcePath = "/images/albums/$coverChecksum.jpg"
        }
    }
}