package utils

import org.junit.jupiter.api.assertNotNull
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.dto.albums.AlbumEditDto
import wtf.ndu.vibin.repos.AlbumRepo

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
}