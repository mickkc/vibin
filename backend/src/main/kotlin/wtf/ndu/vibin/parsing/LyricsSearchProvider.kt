package wtf.ndu.vibin.parsing

interface LyricsSearchProvider {
    suspend fun searchLyrics(searchQuery: String): List<LyricMetadata>?
}