package wtf.ndu.vibin.parsing

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.gson.gson
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.parsers.AlbumSearchProvider
import wtf.ndu.vibin.parsing.parsers.ArtistSearchProvider
import wtf.ndu.vibin.parsing.parsers.FileParser
import wtf.ndu.vibin.parsing.parsers.TrackSearchProvider
import wtf.ndu.vibin.parsing.parsers.deezer.DeezerProvider
import wtf.ndu.vibin.parsing.parsers.itunes.ItunesProvider
import wtf.ndu.vibin.parsing.parsers.lrclib.LrcLibProvider
import wtf.ndu.vibin.parsing.parsers.metadata.MetadataProvider
import wtf.ndu.vibin.parsing.parsers.preparser.PreParser
import wtf.ndu.vibin.parsing.parsers.spotify.SpotifyProvider
import wtf.ndu.vibin.parsing.parsers.theaudiodb.TheAudioDbProvider
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
            gson {
                serializeNulls()
            }
        }
    }

    private val metadataParser = MetadataProvider()
    private val iTunesProvider = ItunesProvider(client)
    private val deezerProvider = DeezerProvider(client)
    private val spotifyProvider = SpotifyProvider(client)
    private val theAudioDbProvider = TheAudioDbProvider(client)

    val fileParsers = mapOf<String, FileParser>(
        "Metadata" to metadataParser,
        "iTunes" to iTunesProvider,
        "Deezer" to deezerProvider,
        "Spotify" to spotifyProvider
    )

    val trackSearchProviders = mapOf<String, TrackSearchProvider>(
        "iTunes" to iTunesProvider,
        "Deezer" to deezerProvider,
        "Spotify" to spotifyProvider,
    )

    val artistSearchProviders = mapOf<String, ArtistSearchProvider>(
        "Deezer" to deezerProvider,
        "TheAudioDb" to theAudioDbProvider,
        "Spotify" to spotifyProvider
    )

    val albumSearchProviders = mapOf<String, AlbumSearchProvider>(
        "Deezer" to deezerProvider,
        "iTunes" to iTunesProvider,
        "Spotify" to spotifyProvider
    )

    val lyricsSearchProviders = mapOf<String, LyricsSearchProvider>(
        "LrcLib" to LrcLibProvider(client)
    )

    /**
     * Tries to parse the given file using the configured primary and fallback metadata sources.
     *
     * @param file The audio file to parse.
     * @return The parsed TrackMetadata, or null if parsing failed with both sources.
     */
    suspend fun parse(file: File): TrackMetadata {

        val sources = listOf(Settings.get(PrimaryMetadataSource), Settings.get(FallbackMetadataSource))
        val preParsed = PreParser.preParse(file)

        if (preParsed != null) {
            for (source in sources) {
                val metadata = fileParsers[source]?.parse(preParsed)
                if (metadata != null) {
                    return TrackMetadata(preParsed, metadata)
                }
            }
        }
        else {
            logger.error("Pre-parsing failed for file: ${file.absolutePath}")
        }

        return TrackMetadata(null, TrackInfoMetadata(
            title = file.nameWithoutExtension,
            artistNames = emptyList(),
            albumName = "Unknown Album",
            explicit = false,
            coverImageUrl = null
        ))
    }

    fun getFileProviders() = fileParsers.keys
    fun getTrackSearchProviders() = trackSearchProviders.keys
    fun getArtistSearchProviders() = artistSearchProviders.keys
    fun getAlbumSearchProviders() = albumSearchProviders.keys
    fun getLyricsSearchProviders() = lyricsSearchProviders.keys

    suspend fun searchTrack(query: String, provider: String): List<TrackInfoMetadata>? {
        val parser = trackSearchProviders[provider] ?: return null

        return try {
            parser.searchTrack(query)
        }
        catch (e: Exception) {
            logger.error("Error searching track '$query' with provider '$provider': ${e.message}", e)
            null
        }
    }

    suspend fun searchArtist(query: String, provider: String): List<ArtistMetadata>? {
        val parser = artistSearchProviders[provider] ?: return null

        return try {
            parser.searchArtist(query)
        }
        catch (e: Exception) {
            logger.error("Error searching artist '$query' with provider '$provider': ${e.message}", e)
            null
        }
    }

    suspend fun searchAlbum(query: String, provider: String): List<AlbumMetadata>? {
        val parser = albumSearchProviders[provider] ?: return null

        return try {
            parser.searchAlbum(query)
        }
        catch (e: Exception) {
            logger.error("Error searching album '$query' with provider '$provider': ${e.message}", e)
            null
        }
    }

    suspend fun searchLyrics(query: String, provider: String): List<LyricMetadata>? {
        val parser = lyricsSearchProviders[provider] ?: return null

        return try {
            parser.searchLyrics(query)
        }
        catch (e: Exception) {
            logger.error("Error searching lyrics for '$query' with provider '$provider': ${e.message}", e)
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