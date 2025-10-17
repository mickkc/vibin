package wtf.ndu.vibin.parsing.parsers.theaudiodb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.parsing.ArtistMetadata
import wtf.ndu.vibin.parsing.parsers.ArtistSearchProvider
import wtf.ndu.vibin.settings.MetadataLanguage
import wtf.ndu.vibin.settings.Settings

class TheAudioDbProvider(val client: HttpClient) : ArtistSearchProvider {

    private val logger: Logger = LoggerFactory.getLogger(TheAudioDbProvider::class.java)

    override suspend fun searchArtist(query: String): List<ArtistMetadata>? {
        try {
            val response = client.get("https://www.theaudiodb.com/api/v1/json/123/search.php") {
                parameter("s", query)
            }

            if (!response.status.isSuccess()) {
                val reply = response.bodyAsText()
                logger.error("TheAudioDb API request failed for artist query '$query': ${response.status}. Response: $reply")
                return null
            }

            val result = response.body<TadbArtistResponse>()
            logger.info("TheAudioDb API response for '$query': found ${result.artists?.size} results")

            return result.artists?.map {
                ArtistMetadata(
                    name = it.strArtist,
                    pictureUrl = it.strArtistThumb?.replace("500x500", "512x512"),
                    biography = when (Settings.get(MetadataLanguage).lowercase()) {
                        "en" -> it.strBiographyEN
                        "de" -> it.strBiographyDE
                        "fr" -> it.strBiographyFR
                        "cn" -> it.strBiographyCN
                        "it" -> it.strBiographyIT
                        "jp" -> it.strBiographyJP
                        "ru" -> it.strBiographyRU
                        "es" -> it.strBiographyES
                        "pt" -> it.strBiographyPT
                        "se" -> it.strBiographySE
                        "nl" -> it.strBiographyNL
                        "hu" -> it.strBiographyHU
                        "no" -> it.strBiographyNO
                        "il" -> it.strBiographyIL
                        "pl" -> it.strBiographyPL
                        else -> it.strBiographyEN
                    } ?: it.strBiographyEN
                )
            }
        } catch (e: Exception) {
            logger.error("Error searching TheAudioDb for artist query '$query'", e)
            return null
        }
    }
}