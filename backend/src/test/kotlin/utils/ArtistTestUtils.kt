package utils

import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.dto.artists.ArtistEditData
import wtf.ndu.vibin.repos.ArtistRepo

object ArtistTestUtils {
    fun createArtist(name: String, sortName: String = name): ArtistEntity {
        return ArtistRepo.updateOrCreateArtist(null, ArtistEditData(name = name, sortName = sortName, imageUrl = null))
    }
}