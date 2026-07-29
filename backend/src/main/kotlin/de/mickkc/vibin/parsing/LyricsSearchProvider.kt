package de.mickkc.vibin.parsing

import de.mickkc.vibin.parsing.parsers.BaseMetadataProvider

interface LyricsSearchProvider : BaseMetadataProvider {
    suspend fun searchLyrics(searchQuery: String): List<LyricMetadata>?

    suspend fun searchLyrics(trackName: String, artistName: String, albumName: String, duration: Long): String?
}