package wtf.ndu.vibin.parsing.parsers.deezer

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.BaseMetadataParser
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackMetadata
import java.io.File

class DeezerParser : BaseMetadataParser() {

    private val logger = LoggerFactory.getLogger(DeezerParser::class.java)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {}
        }
    }

    override suspend fun parseFile(file: File): TrackMetadata? {
        try {
            val fileName = file.nameWithoutExtension
            val response = client.get("https://api.deezer.com/search/track") {
                parameter("q", fileName)
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("Deezer API request failed for file '$fileName': ${response.status}. Response: $reply")
                return null
            }

            val deezerResponse = response.body<DeezerSearchResponse>()
            val track = deezerResponse.data.firstOrNull()
                ?: return null.also { logger.info("No Deezer results for file '$fileName'") }
            logger.info("Deezer API response for '$fileName': found ${deezerResponse.data.size} results")

            val coverUrl = track.album.cover_big.replace("500x500", "512x512")
            val coverImageData = try {
                client.get(coverUrl).bodyAsBytes()
            } catch (e: Exception) {
                logger.warn("Failed to download cover image from $coverUrl", e)
                null
            }

            return TrackMetadata(
                title = track.title,
                artistNames = ParsingUtils.splitArtistNames(track.artist.name),
                albumName = track.album.title,
                durationMs = (track.duration * 1000).toLong(),
                explicit = track.explicit_lyrics,
                coverImageData = coverImageData
            )
        }
        catch (e: Exception) {
            logger.error("Error parsing file ${file.name}", e)
            return null
        }
    }
}
