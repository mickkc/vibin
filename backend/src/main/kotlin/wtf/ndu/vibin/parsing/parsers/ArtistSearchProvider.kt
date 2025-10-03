package wtf.ndu.vibin.parsing.parsers

import wtf.ndu.vibin.parsing.ArtistMetadata

interface ArtistSearchProvider {
    suspend fun searchArtist(query: String): List<ArtistMetadata>?
}