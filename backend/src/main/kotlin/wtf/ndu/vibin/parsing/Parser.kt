package wtf.ndu.vibin.parsing

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.parsers.deezer.DeezerProvider
import wtf.ndu.vibin.parsing.parsers.itunes.ItunesProvider
import wtf.ndu.vibin.parsing.parsers.metadata.MetadataProvider
import wtf.ndu.vibin.settings.FallbackMetadataSource
import wtf.ndu.vibin.settings.PrimaryMetadataSource
import wtf.ndu.vibin.settings.Settings
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Parser {

    private val logger = LoggerFactory.getLogger(Parser::class.java)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
    }

    val parsers = mapOf(
        "Metadata" to MetadataProvider(),
        "Deezer" to DeezerProvider(client),
        "iTunes" to ItunesProvider(client)
    )

    /**
     * Tries to parse the given file using the configured primary and fallback metadata sources.
     *
     * @param file The audio file to parse.
     * @return The parsed TrackMetadata, or null if parsing failed with both sources.
     */
    suspend fun parse(file: File): TrackMetadata {

        val sources = listOf(Settings.get(PrimaryMetadataSource), Settings.get(FallbackMetadataSource))

        for (source in sources) {
            val metadata = parsers[source]?.fromFile(file)
            if (metadata != null) {
                return metadata
            }
        }

        return TrackMetadata(
            title = file.nameWithoutExtension,
            artistNames = emptyList(),
            albumName = "Unknown Album",
            durationMs = null,
            explicit = false,
            coverImageUrl = null
        )
    }

    fun getFileProviders() = parsers.filter { it.value.supportedMethods.fromFile }.keys
    fun getTrackSearchProviders() = parsers.filter { it.value.supportedMethods.searchTrack }.keys
    fun getArtistSearchProviders() = parsers.filter { it.value.supportedMethods.searchArtist }.keys

    suspend fun searchTrack(query: String, provider: String): List<TrackMetadata>? {
        val parser = parsers[provider] ?: return null
        if (!parser.supportedMethods.searchTrack) return null

        return try {
            parser.searchTrack(query)
        }
        catch (e: Exception) {
            logger.error("Error searching track '$query' with provider '$provider': ${e.message}", e)
            null
        }
    }

    suspend fun searchArtist(query: String, provider: String): List<ArtistMetadata>? {
        val parser = parsers[provider] ?: return null
        if (!parser.supportedMethods.searchArtist) return null

        return try {
            parser.searchArtist(query)
        }
        catch (e: Exception) {
            logger.error("Error searching artist '$query' with provider '$provider': ${e.message}", e)
            null
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun downloadCoverImage(url: String): ByteArray? {

        if (url.isBlank()) return null

        if (url.startsWith("data:")) {
            val base64Data = url.substringAfter("base64,")
            return try {
                Base64.decode(base64Data)
            } catch (e: IllegalArgumentException) {
                logger.error("Failed to decode base64 cover image data: ${e.message}", e)
                null
            }
        }

        return try {
            val response = client.get(url)
            if (response.status.isSuccess()) {
                response.bodyAsBytes()
            } else {
                val error = response.bodyAsText()
                logger.error("Failed to download cover image from $url: ${response.status}. Response: $error")
                null
            }
        } catch (e: Exception) {
            logger.error("Error downloading cover image from $url: ${e.message}", e)
            null
        }
    }
}