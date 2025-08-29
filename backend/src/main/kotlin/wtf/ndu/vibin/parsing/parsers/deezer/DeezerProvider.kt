package wtf.ndu.vibin.parsing.parsers.deezer

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.ArtistMetadata
import wtf.ndu.vibin.parsing.BaseMetadataProvider
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackMetadata
import java.io.File

class DeezerProvider(val client: HttpClient) : BaseMetadataProvider() {

    private val logger = LoggerFactory.getLogger(DeezerProvider::class.java)

    override val supportedMethods: SupportedMethods
        get() = SupportedMethods(
            fromFile = true,
            searchTrack = true,
            searchArtist = true
        )

    override suspend fun searchTrack(query: String): List<TrackMetadata>? {

        val deezerResponse = get<DeezerSearchResponse<DeezerTrackData>>("https://api.deezer.com/search/track", query) ?: return null
        logger.info("Deezer API response for track '$query': found ${deezerResponse.data.size} results")

        return deezerResponse.data.map {
            TrackMetadata(
                title = it.title,
                artistNames = ParsingUtils.splitArtistNames(it.artist.name),
                albumName = it.album.title,
                durationMs = (it.duration * 1000).toLong(),
                explicit = it.explicit_lyrics,
                coverImageUrl = it.album.cover_big.replace("500x500", "512x512")
            )
        }
    }

    override suspend fun searchArtist(query: String): List<ArtistMetadata>? {
        val deezerResponse = get<DeezerSearchResponse<DeezerArtistMetadata>>("https://api.deezer.com/search/artist", query) ?: return null
        logger.info("Deezer API response for artist '$query': found ${deezerResponse.data.size} results")

        return deezerResponse.data.map {
            ArtistMetadata(
                name = it.name,
                pictureUrl = it.picture_big?.replace("500x500", "512x512")
            )
        }
    }

    private suspend inline fun <reified T>get(url: String, q: String): T? {
        return try {
            val response = client.get(url) {
                parameter("q", q)
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("Deezer API request failed for URL '$url': ${response.status}. Response: $reply")
                return null
            }

            response.body<T>()
        } catch (e: Exception) {
            logger.error("Deezer API request failed for URL '$url': ${e.message}", e)
            null
        }
    }

    override suspend fun fromFile(file: File): TrackMetadata? = searchTrack(file.nameWithoutExtension)?.firstOrNull()
}
