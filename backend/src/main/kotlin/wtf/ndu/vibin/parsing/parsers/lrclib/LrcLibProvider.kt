package wtf.ndu.vibin.parsing.parsers.lrclib

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.LyricMetadata
import wtf.ndu.vibin.parsing.LyricsSearchProvider

class LrcLibProvider(val client: HttpClient) : LyricsSearchProvider {

    private val logger: Logger = LoggerFactory.getLogger(LrcLibProvider::class.java)

    override suspend fun searchLyrics(searchQuery: String): List<LyricMetadata>? {

        try {
            val response = client.get("https://lrclib.net/api/search") {
                parameter("q", searchQuery)
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("LrcLib API request failed for lyrics query '$searchQuery': ${response.status}. Response: $reply")
                return null
            }

            val lyrics = response.body<List<LrcLibLyrics>>()
            logger.info("LrcLib API response for lyrics query '$searchQuery': found ${lyrics.size} results")

            return lyrics.mapNotNull {
                val content = it.syncedLyrics ?: it.plainLyrics
                if (content == null) return@mapNotNull null
                return@mapNotNull LyricMetadata(
                    title = it.trackName,
                    artistName = it.artistName,
                    albumName = it.albumName,
                    content = content,
                    synced = it.syncedLyrics != null,
                    duration = (it.duration * 1000).toLong(),
                )
            }
        }
        catch (e: Exception) {
            logger.error("LrcLib API request error for lyrics query '$searchQuery': ${e.message}", e)
            return null
        }

    }

}