package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.parsing.AlbumMetadata

interface AlbumSearchProvider {
    suspend fun searchAlbum(query: String): List<AlbumMetadata>?
}