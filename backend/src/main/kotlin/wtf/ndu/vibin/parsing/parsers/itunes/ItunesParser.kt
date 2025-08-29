package wtf.ndu.vibin.parsing.parsers.itunes

import com.google.gson.Gson
import io.ktor.client.*
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

class ItunesParser : BaseMetadataParser() {

    private val logger = LoggerFactory.getLogger(ItunesParser::class.java)
    private val gson  = Gson()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson {}
        }
    }

    override suspend fun parseFile(file: File): TrackMetadata? {

        try {
            val filename = file.nameWithoutExtension
            val response = client.get("https://itunes.apple.com/search") {
                parameter("term", filename)
                parameter("limit", 1)
                parameter("media", "music")
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("iTunes API request failed for file '$filename': ${response.status}. Response: $reply")
                return null
            }

            val itunesResponse = response.bodyAsBytes()
                .toString(Charsets.UTF_8)
                .let { gson.fromJson(it, ItunesSearchResponse::class.java) }

            logger.info("iTunes API raw response for '$filename': $itunesResponse")

            val track = itunesResponse.results.firstOrNull()
                ?: return null.also { logger.info("No iTunes results for file '$filename'") }

            logger.info("iTunes API response for '$filename': found ${itunesResponse.results.size} results")

            val coverUrl = track.artworkUrl100?.replace("100x100bb", "512x512bb")
            val coverImageData = try {
                coverUrl?.let { client.get(it).bodyAsBytes() }
            } catch (e: Exception) {
                logger.warn("Failed to download cover image from $coverUrl", e)
                null
            }

            return TrackMetadata(
                title = track.trackName,
                artistNames = ParsingUtils.splitArtistNames(track.artistName),
                albumName = track.collectionName,
                trackNumber = track.trackNumber,
                trackCount = track.trackCount,
                discNumber = track.discNumber,
                discCount = track.discCount,
                year = track.releaseDate?.substringBefore("-")?.toIntOrNull(),
                genre = track.primaryGenreName,
                durationMs = track.trackTimeMillis,
                explicit = track.trackExplicitness == "explicit",
                coverImageData = coverImageData
            )
        }
        catch (e: Exception) {
            logger.error("Error parsing file ${file.name}", e)
            return null
        }
    }
}