package de.mickkc.vibin.parsing.parsers.spotify

import de.mickkc.vibin.db.UserEntity
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import de.mickkc.vibin.parsing.AlbumMetadata
import de.mickkc.vibin.parsing.ArtistMetadata
import de.mickkc.vibin.parsing.ParsingUtils
import de.mickkc.vibin.parsing.TrackInfoMetadata
import de.mickkc.vibin.parsing.parsers.*
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.SpotifyClientId
import de.mickkc.vibin.settings.server.SpotifyClientSecret
import de.mickkc.vibin.settings.user.SpotifyUserClientId
import de.mickkc.vibin.settings.user.SpotifyUserClientSecret
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class SpotifyProvider(val client: HttpClient) : ArtistSearchProvider, TrackSearchProvider, FileParser, AlbumSearchProvider {

    private val logger: Logger = LoggerFactory.getLogger(SpotifyProvider::class.java)
    private var token: String? = null
    private var tokenExpiry: Long = 0

    fun getClientIdAndSecret(user: UserEntity?): Pair<String, String>? {
        val (cidSource, clientId) = Settings.getUserOrServer(
            userSetting = SpotifyUserClientId,
            serverSetting = SpotifyClientId,
            userId = user?.id?.value,
            permissionType = PermissionType.USE_SERVER_API_CREDENTIALS,
            isValid = String::isNotBlank
        )

        val (csSource, clientSecret) = Settings.getUserOrServer(
            userSetting = SpotifyUserClientSecret,
            serverSetting = SpotifyClientSecret,
            userId = user?.id?.value,
            permissionType = PermissionType.USE_SERVER_API_CREDENTIALS,
            isValid = String::isNotBlank
        )

        // Check that both client id and secret are set and from the same source
        // If the user has set an id, but the secret is loaded from the server, it will not work.
        if (clientId == null || clientSecret == null || cidSource != csSource) {
            return null
        }

        return clientId to clientSecret
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun updateAccessToken(user: UserEntity?): String {
        if (token != null && System.currentTimeMillis() < tokenExpiry) {
            return token!!
        }

        val loadedCredentials = getClientIdAndSecret(user)
            ?: throw IllegalStateException("Spotify Client ID or Client Secret is not set in settings.")

        val credentials = Base64.encode("${loadedCredentials.first}:${loadedCredentials.second}".toByteArray())

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

    override suspend fun searchArtist(query: String, user: UserEntity?): List<ArtistMetadata>? {

        try {
            val accessToken = updateAccessToken(user)

            val response = client.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
                parameter("type", "artist")
                ParsingUtils.limit?.let {
                    parameter("limit", it)
                }
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

    override suspend fun searchTrack(query: String, user: UserEntity?): List<TrackInfoMetadata>? {
        try {
            val accessToken = updateAccessToken(user)

            val response = client.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
                parameter("type", "track")
                ParsingUtils.limit?.let {
                    parameter("limit", it)
                }
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
                    artists = it.artists.map { artist -> artist.name },
                    album = it.album.name,
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

    override suspend fun parse(data: PreparseData, user: UserEntity?): TrackInfoMetadata? {
        return searchTrack(data.audioFile.file.nameWithoutExtension, user)?.firstOrNull()
    }

    override suspend fun searchAlbum(query: String, user: UserEntity?): List<AlbumMetadata>? {

        try {
            val accessToken = updateAccessToken(user)

            val response = client.get("https://api.spotify.com/v1/search") {
                header("Authorization", "Bearer $accessToken")
                parameter("q", query)
                parameter("type", "album")
                ParsingUtils.limit?.let {
                    parameter("limit", it)
                }
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
                    description = null,
                    isSingle = it.album_type == "single"
                )
            }
        }
        catch (e: Exception) {
            logger.error("Error searching Spotify for album query '$query': ${e.message}", e)
            return null
        }
    }

    override fun isAvailable(user: UserEntity?): Boolean {
        return getClientIdAndSecret(user) != null
    }
}