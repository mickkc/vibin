package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.parsing.ArtistMetadata

interface ArtistSearchProvider : BaseMetadataProvider {
    suspend fun searchArtist(query: String, user: UserEntity?): List<ArtistMetadata>?
}