package de.mickkc.vibin.parsing

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import org.slf4j.LoggerFactory
import de.mickkc.vibin.parsing.parsers.AlbumSearchProvider
import de.mickkc.vibin.parsing.parsers.ArtistSearchProvider
import de.mickkc.vibin.parsing.parsers.BaseMetadataProvider
import de.mickkc.vibin.parsing.parsers.FileParser
import de.mickkc.vibin.parsing.parsers.TrackSearchProvider
import de.mickkc.vibin.parsing.parsers.deezer.DeezerProvider
import de.mickkc.vibin.parsing.parsers.itunes.ItunesProvider
import de.mickkc.vibin.parsing.parsers.lastfm.LastFmProvider
import de.mickkc.vibin.parsing.parsers.lrclib.LrcLibProvider
import de.mickkc.vibin.parsing.parsers.metadata.MetadataProvider
import de.mickkc.vibin.parsing.parsers.preparser.PreParser
import de.mickkc.vibin.parsing.parsers.spotify.SpotifyProvider
import de.mickkc.vibin.parsing.parsers.spotify.SpotifyScrapingProvider
import de.mickkc.vibin.parsing.parsers.theaudiodb.TheAudioDbProvider
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.settings.server.FallbackMetadataSource
import de.mickkc.vibin.settings.server.PrimaryMetadataSource
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.AlbumMetadataFetchType
import de.mickkc.vibin.settings.server.ArtistMetadataFetchType
import de.mickkc.vibin.settings.server.ArtistMetadataSource
import de.mickkc.vibin.settings.server.LyricsMetadataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.MalformedURLException
import java.net.URL
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
    private val spotifyScrapingProvider = SpotifyScrapingProvider()
    private val theAudioDbProvider = TheAudioDbProvider(client)
    private val lastFmProvider = LastFmProvider(client)

    val fileParsers = mapOf<String, FileParser>(
        "Metadata" to metadataParser,
        "iTunes" to iTunesProvider,
        "Deezer" to deezerProvider,
        "Spotify" to spotifyProvider,
        "Spotify (Scraping)" to spotifyScrapingProvider,
        "Last.fm" to lastFmProvider
    )

    val trackSearchProviders = mapOf<String, TrackSearchProvider>(
        "iTunes" to iTunesProvider,
        "Deezer" to deezerProvider,
        "Spotify" to spotifyProvider,
        "Spotify (Scraping)" to spotifyScrapingProvider,
        "Last.fm" to lastFmProvider
    )

    val artistSearchProviders = mapOf<String, ArtistSearchProvider>(
        "Deezer" to deezerProvider,
        "TheAudioDb" to theAudioDbProvider,
        "Spotify" to spotifyProvider,
        "Spotify (Scraping)" to spotifyScrapingProvider,
        "Last.fm" to lastFmProvider
    )

    val albumSearchProviders = mapOf<String, AlbumSearchProvider>(
        "Deezer" to deezerProvider,
        "iTunes" to iTunesProvider,
        "Spotify" to spotifyProvider,
        "Spotify (Scraping)" to spotifyScrapingProvider,
        "Last.fm" to lastFmProvider
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

        for (source in sources) {
            val metadata = fileParsers[source]?.parse(preParsed)
            if (metadata != null) {
                return TrackMetadata(preParsed, metadata)
            }
        }

        return TrackMetadata(null, TrackInfoMetadata(
            title = file.nameWithoutExtension,
            artists = emptyList(),
            album = AlbumRepo.UNKNOWN_ALBUM_NAME,
            explicit = false,
            coverImageUrl = null
        ))
    }

    /**
     * Returns the keys (names) of all providers inside the Map that are available.
     *
     * @param providers A map containing all providers
     * @return The keys of all available providers.
     */
    private fun getAvailableProviderKeys(providers: Map<String, BaseMetadataProvider>): Set<String> {
        return providers.mapNotNull { if (it.value.isAvailable()) it.key else null }.toSet()
    }

    fun getAvailableFileProviders() = getAvailableProviderKeys(fileParsers)
    fun getAvailableTrackSearchProviders() =  getAvailableProviderKeys(trackSearchProviders)
    fun getAvailableArtistSearchProviders() = getAvailableProviderKeys(artistSearchProviders)
    fun getAvailableAlbumSearchProviders() = getAvailableProviderKeys(albumSearchProviders)
    fun getAvailableLyricsSearchProviders() = getAvailableProviderKeys(lyricsSearchProviders)

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

    suspend fun searchArtistAuto(query: String): ArtistMetadata? {
        val type = Settings.get(ArtistMetadataFetchType)
        if (type == MetadataFetchingType.NONE) return null

        val provider = Settings.get(ArtistMetadataSource)
        val parser = artistSearchProviders[provider] ?: return null

        return try {
            val results = parser.searchArtist(query)
            if (results == null || results.isEmpty()) {
                null
            } else {
                when (type) {
                    MetadataFetchingType.EXACT_MATCH -> results.firstOrNull { it.name == query }
                    MetadataFetchingType.CASE_INSENSITIVE_MATCH -> results.firstOrNull { it.name.equals(query, ignoreCase = true) }
                    MetadataFetchingType.FIRST_RESULT -> results.first()
                    else -> null
                }
            }
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

    suspend fun searchAlbumAuto(query: String): AlbumMetadata? {
        val type = Settings.get(AlbumMetadataFetchType)
        if (type == MetadataFetchingType.NONE) return null

        val provider = Settings.get(ArtistMetadataSource)
        val parser = albumSearchProviders[provider] ?: return null

        return try {
            val results = parser.searchAlbum(query)
            if (results == null || results.isEmpty()) {
                null
            } else {
                when (type) {
                    MetadataFetchingType.EXACT_MATCH -> results.firstOrNull { it.title == query }
                    MetadataFetchingType.CASE_INSENSITIVE_MATCH -> results.firstOrNull { it.title.equals(query, ignoreCase = true) }
                    MetadataFetchingType.FIRST_RESULT -> results.first()
                    else -> null
                }
            }
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

    suspend fun searchLyricsAuto(trackMetadata: TrackMetadata): String? {

        if (trackMetadata.trackInfo.artists.isNullOrEmpty() || trackMetadata.trackInfo.album == null || trackMetadata.fileInfo?.durationMs == null) {
            return null
        }

        val provider = Settings.get(LyricsMetadataSource)
        val parser = lyricsSearchProviders[provider] ?: return null

        return try {
            parser.searchLyrics(
                trackName = trackMetadata.trackInfo.title,
                artistName = trackMetadata.trackInfo.artists.first(),
                albumName = trackMetadata.trackInfo.album,
                duration = trackMetadata.fileInfo.durationMs
            )
        }
        catch (e: Exception) {
            logger.error("Error searching lyrics for '${trackMetadata.trackInfo.title}' by '${trackMetadata.trackInfo.artists.first()}' with provider '$provider': ${e.message}", e)
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

        try {
            val parsedUrl = URL(url)
            if (parsedUrl.protocol !in listOf("http", "https")) {
                logger.warn("Non-http(s) url provided to downloadCoverImage: $url")
                return null
            }

            val addr = withContext(Dispatchers.IO) {
                InetAddress.getByName(parsedUrl.host)
            }
            if (addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress) {
                logger.warn("Local/Loopback address provided to downloadCoverImage: $url")
                return null
            }
        }
        catch (e: MalformedURLException) {
            logger.warn("Malformed url provided to downloadCoverImage: $url", e)
            return null
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