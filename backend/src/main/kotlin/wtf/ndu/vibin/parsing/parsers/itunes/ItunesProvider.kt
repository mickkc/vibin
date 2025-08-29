package wtf.ndu.vibin.parsing.parsers.itunes

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.BaseMetadataProvider
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackMetadata
import java.io.File

class ItunesProvider(val client: HttpClient) : BaseMetadataProvider() {

    private val logger = LoggerFactory.getLogger(ItunesProvider::class.java)
    private val gson  = Gson()

    override val supportedMethods: SupportedMethods
        get() = SupportedMethods(
            fromFile = true,
            searchTrack = true,
            searchArtist = false
        )

    override suspend fun searchTrack(query: String): List<TrackMetadata>? {

        try {
            val response = client.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("limit", 1)
                parameter("media", "music")
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("iTunes API request failed for query '$query': ${response.status}. Response: $reply")
                return null
            }

            val itunesResponse = response.bodyAsBytes()
                .toString(Charsets.UTF_8)
                .let { gson.fromJson(it, ItunesSearchResponse::class.java) }

            logger.info("iTunes API response for '$query': found ${itunesResponse.results.size} results")

            return itunesResponse.results.map {
                TrackMetadata(
                    title = it.trackName,
                    artistNames = ParsingUtils.splitArtistNames(it.artistName),
                    albumName = it.collectionName,
                    trackNumber = it.trackNumber,
                    trackCount = it.trackCount,
                    discNumber = it.discNumber,
                    discCount = it.discCount,
                    year = it.releaseDate?.substringBefore("-")?.toIntOrNull(),
                    genre = it.primaryGenreName,
                    durationMs = it.trackTimeMillis,
                    explicit = it.trackExplicitness == "explicit",
                    coverImageUrl = it.artworkUrl100?.replace("100x100bb", "512x512bb")
                )
            }
        }
        catch (e: Exception) {
            logger.error("Error searching iTunes for query '$query'", e)
            return null
        }
    }

    override suspend fun fromFile(file: File): TrackMetadata? = searchTrack(file.nameWithoutExtension)?.firstOrNull()
}