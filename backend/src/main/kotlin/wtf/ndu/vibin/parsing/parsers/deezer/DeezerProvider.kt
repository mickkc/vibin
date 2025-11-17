package wtf.ndu.vibin.parsing.parsers.deezer

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.AlbumMetadata
import wtf.ndu.vibin.parsing.ArtistMetadata
import wtf.ndu.vibin.parsing.ParsingUtils
import wtf.ndu.vibin.parsing.TrackInfoMetadata
import wtf.ndu.vibin.parsing.parsers.AlbumSearchProvider
import wtf.ndu.vibin.parsing.parsers.ArtistSearchProvider
import wtf.ndu.vibin.parsing.parsers.FileParser
import wtf.ndu.vibin.parsing.parsers.PreparseData
import wtf.ndu.vibin.parsing.parsers.TrackSearchProvider
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.server.ExtendedMetadata

class DeezerProvider(val client: HttpClient) : FileParser, ArtistSearchProvider, TrackSearchProvider, AlbumSearchProvider {

    private val logger = LoggerFactory.getLogger(DeezerProvider::class.java)

    override suspend fun searchTrack(query: String): List<TrackInfoMetadata>? {

        val deezerResponse = get<DeezerSearchResponse<DeezerTrackData>>("https://api.deezer.com/search/track", query) ?: return null
        logger.info("Deezer API response for track '$query': found ${deezerResponse.data.size} results")

        val extended = Settings.get(ExtendedMetadata)

        return deezerResponse.data.map {

            val mapped = if (extended) {
                getExtendedTrackMetadata(it.id)
            } else null

            mapped ?: TrackInfoMetadata(
                title = it.title,
                artists = ParsingUtils.splitArtistNames(it.artist.name),
                album = it.album.title,
                explicit = it.explicit_lyrics,
                coverImageUrl = it.album.cover_xl
            )
        }
    }

    private suspend fun getExtendedTrackMetadata(trackId: Long): TrackInfoMetadata? {
        val deezerTrackInfo = get<DeezerTrackInfo>("https://api.deezer.com/track/$trackId", "") ?: return null

        return TrackInfoMetadata(
            title = deezerTrackInfo.title,
            artists = deezerTrackInfo.contributors.map { it.name },
            album = deezerTrackInfo.album.title,
            trackNumber = deezerTrackInfo.track_position,
            discNumber = deezerTrackInfo.disk_number,
            year = deezerTrackInfo.release_date.split("-").firstOrNull()?.toIntOrNull(),
            explicit = deezerTrackInfo.explicit_lyrics,
            coverImageUrl = deezerTrackInfo.album.cover_xl
        )
    }

    override suspend fun searchArtist(query: String): List<ArtistMetadata>? {
        val deezerResponse = get<DeezerSearchResponse<DeezerArtistMetadata>>("https://api.deezer.com/search/artist", query) ?: return null
        logger.info("Deezer API response for artist '$query': found ${deezerResponse.data.size} results")

        return deezerResponse.data.map {
            ArtistMetadata(
                name = it.name,
                pictureUrl = it.picture_xl
            )
        }
    }

    override suspend fun searchAlbum(query: String): List<AlbumMetadata>? {
        val deezerResponse = get<DeezerSearchResponse<DeezerAlbumMetadata>>("https://api.deezer.com/search/album", query) ?: return null
        logger.info("Deezer API response for album '$query': found ${deezerResponse.data.size} results")

        return deezerResponse.data.map {
            AlbumMetadata(
                title = it.title,
                coverImageUrl = it.cover_xl,
                artistName = it.artist?.name,
                description = null,
                isSingle = it.record_type == "single"
            )
        }
    }

    private suspend inline fun <reified T>get(url: String, q: String): T? {
        return try {
            val response = client.get(url) {
                parameter("q", q)
                ParsingUtils.limit?.let {
                    parameter("limit", it)
                }
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

    override suspend fun parse(data: PreparseData): TrackInfoMetadata? = searchTrack(data.audioFile.file.nameWithoutExtension)?.firstOrNull()
}
