package utils

import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.dto.artists.ArtistEditData
import de.mickkc.vibin.repos.ArtistRepo

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