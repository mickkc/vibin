package de.mickkc.vibin.parsing.parsers.itunes

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import de.mickkc.vibin.parsing.AlbumMetadata
import de.mickkc.vibin.parsing.ParsingUtils
import de.mickkc.vibin.parsing.TrackInfoMetadata
import de.mickkc.vibin.parsing.parsers.AlbumSearchProvider
import de.mickkc.vibin.parsing.parsers.FileParser
import de.mickkc.vibin.parsing.parsers.PreparseData
import de.mickkc.vibin.parsing.parsers.TrackSearchProvider

class ItunesProvider(val client: HttpClient) : TrackSearchProvider, AlbumSearchProvider, FileParser {

    private val logger = LoggerFactory.getLogger(ItunesProvider::class.java)
    private val json  = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun searchTrack(query: String): List<TrackInfoMetadata>? {

        try {
            val response = client.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("media", "music")
                parameter("entity", "song")
                ParsingUtils.limit?.let {
                    parameter("limit", it)
                }
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("iTunes API request failed for query '$query': ${response.status}. Response: $reply")
                return null
            }

            val itunesResponse = response.bodyAsBytes()
                .toString(Charsets.UTF_8)
                .let { json.decodeFromString<ItunesSearchResponse<ItunesTrackData>>(it) }

            logger.info("iTunes API response for '$query': found ${itunesResponse.results.size} results")

            return itunesResponse.results.map {
                TrackInfoMetadata(
                    title = it.trackName,
                    artists = ParsingUtils.splitArtistNames(it.artistName),
                    album = it.collectionName,
                    trackNumber = it.trackNumber,
                    trackCount = it.trackCount,
                    discNumber = it.discNumber,
                    discCount = it.discCount,
                    year = it.releaseDate?.substringBefore("-")?.toIntOrNull(),
                    tags = it.primaryGenreName?.let { genre -> listOf(genre) },
                    explicit = it.trackExplicitness == "explicit",
                    coverImageUrl = it.artworkUrl100?.replace("100x100bb", "1024x1024bb")
                )
            }
        }
        catch (e: Exception) {
            logger.error("Error searching iTunes for query '$query'", e)
            return null
        }
    }

    override suspend fun searchAlbum(query: String): List<AlbumMetadata>? {
        try {
            val response = client.get("https://itunes.apple.com/search") {
                parameter("term", query)
                parameter("media", "music")
                parameter("entity", "album")
                ParsingUtils.limit?.let {
                    parameter("limit", it)
                }
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("iTunes API request failed for album query '$query': ${response.status}. Response: $reply")
                return null
            }

            val itunesResponse = response.bodyAsBytes()
                .toString(Charsets.UTF_8)
                .let { json.decodeFromString<ItunesSearchResponse<ItunesAlbumData>>(it) }

            logger.info("iTunes API response for album '$query': found ${itunesResponse.results.size} results")

            return itunesResponse.results.map {
                AlbumMetadata(
                    title = it.collectionName,
                    artistName = it.artistName,
                    year = it.releaseDate.substringBefore("-").toIntOrNull(),
                    coverImageUrl = it.artworkUrl100?.replace("100x100bb", "512x512bb"),
                    description = null,
                    isSingle = it.collectionName.endsWith(" - Single")
                )
            }
        }
        catch (e: Exception) {
            logger.error("Error searching iTunes for album query '$query'", e)
            return null
        }
    }

    override suspend fun parse(data: PreparseData): TrackInfoMetadata? = searchTrack(data.audioFile.file.nameWithoutExtension)?.firstOrNull()
}