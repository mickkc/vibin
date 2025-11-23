package de.mickkc.vibin.parsing

interface LyricsSearchProvider {
    suspend fun searchLyrics(searchQuery: String): List<LyricMetadata>?

    suspend fun searchLyrics(trackName: String, artistName: String, albumName: String, duration: Long): String?
}