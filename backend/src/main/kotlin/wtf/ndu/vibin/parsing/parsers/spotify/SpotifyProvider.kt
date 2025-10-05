package wtf.ndu.vibin.parsing.parsers.spotify

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.AlbumMetadata
import wtf.ndu.vibin.parsing.ArtistMetadata
import wtf.ndu.vibin.parsing.TrackInfoMetadata
import wtf.ndu.vibin.parsing.parsers.*
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.SpotifyClientId
import wtf.ndu.vibin.settings.SpotifyClientSecret
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class SpotifyProvider(val client: HttpClient) : ArtistSearchProvider, TrackSearchProvider, FileParser, AlbumSearchProvider {

    private val logger: Logger = LoggerFactory.getLogger(SpotifyProvider::class.java)
    private var token: String? = null
    private var tokenExpiry: Long = 0

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun updateAccessToken(): String {
        if (token != null && System.currentTimeMillis() < tokenExpiry) {
            return token!!
        }

        val clientId = Settings.get(SpotifyClientId)
        val clientSecret = Settings.get(SpotifyClientSecret)

        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw IllegalStateException("Spotify Client ID or Client Secret is not set in settings.")
        }

        val credentials = Base64.encode("$clientId:$clientSecret".toByteArray())

        val response = client.post("https://accounts.spotify.com/api/token") {
            header("Authorization", "Basic $credentials")
            header("Content-Type", "application/x-www-form-urlencoded")
            setBody("grant_type=client_credentials")
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            throw Exception("Spotify token request failed: ${response.status}. Response: $reply")
        }

        val tokenResponse = response.body<SpotifyAccessToken>()
        token = tokenResponse.access_token
        tokenExpiry = System.currentTimeMillis() + (tokenResponse.expires_in - 60) * 1000
        return token!!
    }

    override suspend fun searchArtist(query: String): List<ArtistMetadata>? {

        try {
            val accessToken = updateAccessToken()

            val response = client.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
                parameter("type", "artist")
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("Spotify API request failed for artist query '$query': ${response.status}. Response: $reply")
                return null
            }

            val result = response.body<SpotifyArtistResponse>().artists
            logger.info("Spotify API response for artist '$query': found ${result.items.size} results")

            return result.items.map {
                ArtistMetadata(
                    name = it.name,
                    pictureUrl = it.images.maxByOrNull { img -> img.width * img.height }?.url
                )
            }
        } catch (e: Exception) {
            logger.error("Error searching Spotify for query '$query': ${e.message}", e)
            return null
        }
    }

    override suspend fun searchTrack(query: String): List<TrackInfoMetadata>? {
        try {
            val accessToken = updateAccessToken()

            val response = client.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
                parameter("type", "track")
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("Spotify API request failed for track query '$query': ${response.status}. Response: $reply")
                return null
            }

            val result = response.body<SpotifyTrackResponse>().tracks
            logger.info("Spotify API response for track '$query': found ${result.items.size} results")

            return result.items.map {
                TrackInfoMetadata(
                    title = it.name,
                    artistNames = it.artists.map { artist -> artist.name },
                    albumName = it.album.name,
                    trackNumber = it.track_number,
                    trackCount = if (it.album.album_type == "single") 1 else it.album.total_tracks,
                    discNumber = it.disc_number,
                    year = it.album.release_date.substringBefore("-").toIntOrNull(),
                    explicit = it.explicit,
                    coverImageUrl = it.album.images.maxByOrNull { img -> img.width * img.height }?.url
                )
            }
        } catch (e: Exception) {
            logger.error("Error searching Spotify for track query '$query': ${e.message}", e)
            return null
        }
    }

    override suspend fun parse(data: PreparseData): TrackInfoMetadata? {
        return searchTrack(data.audioFile.file.nameWithoutExtension)?.firstOrNull()
    }

    override suspend fun searchAlbum(query: String): List<AlbumMetadata>? {

        try {
            val accessToken = updateAccessToken()

            val response = client.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
                parameter("type", "album")
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("Spotify API request failed for album query '$query': ${response.status}. Response: $reply")
                return null
            }

            val result = response.body<SpotifyAlbumResponse>().albums
            logger.info("Spotify API response for album '$query': found ${result.items.size} results")

            return result.items.map {
                AlbumMetadata(
                    title = it.name,
                    artistName = it.artists.joinToString { a -> a.name },
                    year = it.release_date.substringBefore("-").toIntOrNull(),
                    coverImageUrl = it.images.maxByOrNull { img -> img.width * img.height }?.url,
                    description = null
                )
            }
        }
        catch (e: Exception) {
            logger.error("Error searching Spotify for album query '$query': ${e.message}", e)
            return null
        }
    }
}