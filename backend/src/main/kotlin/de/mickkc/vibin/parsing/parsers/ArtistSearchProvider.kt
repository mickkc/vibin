package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.parsing.ArtistMetadata

interface ArtistSearchProvider {
    suspend fun searchArtist(query: String): List<ArtistMetadata>?
}