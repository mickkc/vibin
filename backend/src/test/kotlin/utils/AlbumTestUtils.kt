package utils

import org.junit.jupiter.api.assertNotNull
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.dto.albums.AlbumEditDto
import wtf.ndu.vibin.repos.AlbumRepo

object AlbumTestUtils {

    fun createAlbum(title: String, description: String, year: Int? = null, isSingle: Boolean = false): AlbumEntity {
        val album = AlbumRepo.getOrCreateAlbum(title)
        val updatedAlbum = AlbumRepo.update(album.id.value, AlbumEditDto(
            title = title,
            description = description,
            year = year,
            coverUrl = null,
            isSingle = isSingle
        ))
        assertNotNull(updatedAlbum)
        return updatedAlbum
    }
}