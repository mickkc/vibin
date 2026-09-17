package de.mickkc.vibin.parsing

import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.parsing.parsers.BaseMetadataProvider

interface LyricsSearchProvider : BaseMetadataProvider {
    suspend fun searchLyrics(searchQuery: String, user: UserEntity?): List<LyricMetadata>?

    suspend fun searchLyrics(trackName: String, artistName: String, albumName: String, duration: Long, user: UserEntity?): String?
}