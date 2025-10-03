package wtf.ndu.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import wtf.ndu.vibin.parsing.ArtistMetadata
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.parsing.TrackInfoMetadata

fun Application.configureMetadataRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/metadata/providers") {
            call.respond(mapOf(
                "file" to Parser.getFileProviders(),
                "track" to Parser.getTrackSearchProviders(),
                "artist" to Parser.getArtistSearchProviders(),
                "album" to Parser.getAlbumSearchProviders()
            ))
        }

        get("/api/metadata/track") {
            val query = call.request.queryParameters["q"]?.takeIf { it.isNotBlank() } ?: return@get call.missingParameter("q")
            val provider = call.request.queryParameters["provider"] ?: return@get call.missingParameter("provider")

            val results = Parser.searchTrack(query, provider) ?: return@get call.respond(emptyList<TrackInfoMetadata>())
            call.respond(results)
        }

        get("/api/metadata/artist") {
            val query = call.request.queryParameters["q"]?.takeIf { it.isNotBlank() } ?: return@get call.missingParameter("q")
            val provider = call.request.queryParameters["provider"] ?: return@get call.missingParameter("provider")

            val results = Parser.searchArtist(query, provider) ?: return@get call.respond(emptyList<ArtistMetadata>())
            call.respond(results)
        }

        get("/api/metadata/album") {
            val query = call.request.queryParameters["q"]?.takeIf { it.isNotBlank() } ?: return@get call.missingParameter("q")
            val provider = call.request.queryParameters["provider"] ?: return@get call.missingParameter("provider")

            val results = Parser.searchAlbum(query, provider) ?: return@get call.respond(emptyList<ArtistMetadata>())
            call.respond(results)
        }
    }

}