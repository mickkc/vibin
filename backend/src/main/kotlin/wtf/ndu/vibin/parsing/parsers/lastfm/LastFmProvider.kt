package wtf.ndu.vibin.parsing.parsers.lastfm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.AlbumMetadata
import wtf.ndu.vibin.parsing.ArtistMetadata
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackInfoMetadata
import wtf.ndu.vibin.parsing.parsers.*
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.server.ExtendedMetadata
import wtf.ndu.vibin.settings.server.LastFmApiKey

class LastFmProvider(val client: HttpClient) : TrackSearchProvider, ArtistSearchProvider, AlbumSearchProvider, FileParser {

    private val logger: Logger = LoggerFactory.getLogger(LastFmProvider::class.java)

    private fun getApiKey(): String {
        val apiKey = Settings.get(LastFmApiKey)
        if (apiKey.isEmpty()) {
            throw IllegalStateException("Last.fm API key is not set in settings")
        }
        return apiKey
    }

    private suspend fun getExtendedTrackMetadata(artist: String, track: String): TrackInfoMetadata? {

        val response = client.get("https://ws.audioscrobbler.com/2.0") {
            parameter("method", "track.getinfo")
            parameter("artist", artist)
            parameter("track", track)
            parameter("api_key", getApiKey())
            parameter("format", "json")
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            logger.error("Last.fm API request failed for track info '$artist - $track': ${response.status}. Response: $reply")
            return null
        }

        val result = response.body<LastFmTrackInfo>()

        return TrackInfoMetadata(
            title = result.track.name,
            artists = ParsingUtils.splitArtistNames(result.track.artist.name),
            album = result.track.album.title,
            coverImageUrl = result.track.album.let { getImageUrl(it.image) },
            tags = result.track.toptags.tag.map { it.name },
            comment = result.track.wiki?.summary,
        )
    }

    override suspend fun searchTrack(query: String): List<TrackInfoMetadata>? {

        val response = client.get("https://ws.audioscrobbler.com/2.0") {
            parameter("method", "track.search")
            parameter("track", query)
            parameter("api_key", getApiKey())
            parameter("format", "json")
            ParsingUtils.limit?.let {parameter("limit", it) }
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            logger.error("Last.fm API request failed for track search query '$query': ${response.status}. Response: $reply")
            return null
        }

        val result = response.body<LastFmResults<LastFmTrackResults>>()

        logger.info("Last.fm API response for track search query '$query': found ${result.results.trackmatches.track.size} results")

        val extended = Settings.get(ExtendedMetadata)

        return result.results.trackmatches.track.map { track ->
            var mapped: TrackInfoMetadata? = null

            if (extended) {
                mapped = getExtendedTrackMetadata(track.artist, track.name)
            }
            mapped ?: TrackInfoMetadata(
                title = track.name,
                artists = ParsingUtils.splitArtistNames(track.artist),
                album = null,
                coverImageUrl = getImageUrl(track.image),
            )
        }
    }

    private suspend fun getExtendedArtistMetadata(artist: String): ArtistMetadata? {
        val response = client.get("https://ws.audioscrobbler.com/2.0") {
            parameter("method", "artist.getinfo")
            parameter("artist", artist)
            parameter("api_key", getApiKey())
            parameter("format", "json")
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            logger.error("Last.fm API request failed for artist info '$artist': ${response.status}. Response: $reply")
            return null
        }

        val result = response.body<LastFmArtistInfo>()

        return ArtistMetadata(
            name = result.artist.name,
            pictureUrl = getImageUrl(result.artist.image),
            biography = result.artist.bio?.summary
        )
    }

    override suspend fun searchArtist(query: String): List<ArtistMetadata>? {
        val response = client.get("https://ws.audioscrobbler.com/2.0") {
            parameter("method", "artist.search")
            parameter("artist", query)
            parameter("api_key", getApiKey())
            parameter("format", "json")
            ParsingUtils.limit?.let {parameter("limit", it) }
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            logger.error("Last.fm API request failed for artist search query '$query': ${response.status}. Response: $reply")
            return null
        }

        val result = response.body<LastFmResults<LastFmArtistResults>>()

        logger.info("Last.fm API response for artist search query '$query': found ${result.results.artistmatches.artist.size} results")

        val extended = Settings.get(ExtendedMetadata)

        return result.results.artistmatches.artist.map { artist ->

            val mapped: ArtistMetadata? = if (extended) {
                getExtendedArtistMetadata(artist.name)
            } else null

            mapped ?: ArtistMetadata(
                name = artist.name,
                pictureUrl = getImageUrl(artist.image),
                biography = null
            )
        }
    }

    private fun getImageUrl(images: List<LastFmImage>): String? {
        return images.find { it.size == "extralarge" }?.`#text`?.replace("/300x300/", "/1024x1024/")
    }

    private suspend fun getExtendedAlbumMetadata(artist: String, album: String): AlbumMetadata? {

        val response = client.get("https://ws.audioscrobbler.com/2.0") {
            parameter("method", "album.getinfo")
            parameter("artist", artist)
            parameter("album", album)
            parameter("api_key", getApiKey())
            parameter("format", "json")
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            logger.error("Last.fm API request failed for album info '$artist - $album': ${response.status}. Response: $reply")
            return null
        }

        val result = response.body<LastFmAlbumInfo>()

        return AlbumMetadata(
            title = result.album.name,
            description = result.album.wiki?.summary,
            artistName = result.album.artist,
            coverImageUrl = getImageUrl(result.album.image),
        )
    }

    override suspend fun searchAlbum(query: String): List<AlbumMetadata>? {

        val response = client.get("https://ws.audioscrobbler.com/2.0") {
            parameter("method", "album.search")
            parameter("album", query)
            parameter("api_key", getApiKey())
            parameter("format", "json")
            ParsingUtils.limit?.let { parameter("limit", it) }
        }

        if (!response.status.isSuccess()) {
            val reply = response.bodyAsText()
            logger.error("Last.fm API request failed for album search query '$query': ${response.status}. Response: $reply")
            return null
        }

        val result = response.body<LastFmResults<LastFmAlbumResults>>()

        logger.info("Last.fm API response for album search query '$query': found ${result.results.albummatches.album.size} results")

        val extended = Settings.get(ExtendedMetadata)

        return result.results.albummatches.album.map { album ->

            val mapped: AlbumMetadata? = if (extended) {
                getExtendedAlbumMetadata(album.artist, album.name)
            } else null

            mapped ?: AlbumMetadata(
                title = album.name,
                description = null,
                artistName = album.artist,
                coverImageUrl = getImageUrl(album.image)
            )
        }
    }

    override suspend fun parse(data: PreparseData): TrackInfoMetadata? {
        return searchTrack(data.audioFile.file.nameWithoutExtension)?.firstOrNull()
    }
}