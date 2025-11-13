package utils

import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.dto.artists.ArtistEditData
import wtf.ndu.vibin.repos.ArtistRepo

object ArtistTestUtils {
    suspend fun createArtist(name: String, description: String = ""): ArtistEntity {
        return ArtistRepo.updateOrCreateArtist(null, ArtistEditData(name = name, description = description, imageUrl = null))
    }

    suspend fun getOrCreateArtist(name: String): ArtistEntity {
        val existingArtist = ArtistRepo.getByName(name)
        if (existingArtist != null) {
            return existingArtist
        }
        return createArtist(name)
    }
}