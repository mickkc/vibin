package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.parsing.AlbumMetadata

interface AlbumSearchProvider : BaseMetadataProvider {
    suspend fun searchAlbum(query: String, user: UserEntity?): List<AlbumMetadata>?
}