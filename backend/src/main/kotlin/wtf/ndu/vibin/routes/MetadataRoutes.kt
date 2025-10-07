package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.parsing.Parser

fun Application.configureMetadataRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/metadata/providers") {
            call.respond(mapOf(
                "file" to Parser.getFileProviders(),
                "track" to Parser.getTrackSearchProviders(),
                "artist" to Parser.getArtistSearchProviders(),
                "album" to Parser.getAlbumSearchProviders(),
                "lyrics" to Parser.getLyricsSearchProviders()
            ))
        }

        get("/api/metadata/{type}") {
            val query = call.request.queryParameters["q"]?.takeIf { it.isNotBlank() } ?: return@get call.missingParameter("q")
            val provider = call.request.queryParameters["provider"] ?: return@get call.missingParameter("provider")

            val results = when (call.parameters["type"]) {
                "track" -> Parser.searchTrack(query, provider)
                "artist" -> Parser.searchArtist(query, provider)
                "album" -> Parser.searchAlbum(query, provider)
                "lyrics" -> Parser.searchLyrics(query, provider)
                else -> return@get call.invalidParameter("type", "track", "artist", "album", "lyrics")
            } ?: return@get call.respond(emptyList<Any>())
            call.respond(results)
        }
    }
}
