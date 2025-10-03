package wtf.ndu.vibin.parsing.parsers

import wtf.ndu.vibin.parsing.AlbumMetadata

interface AlbumSearchProvider {
    suspend fun searchAlbum(query: String): List<AlbumMetadata>?
}