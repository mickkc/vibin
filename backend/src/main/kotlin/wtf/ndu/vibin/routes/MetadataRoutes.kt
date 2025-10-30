package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.CreateMetadataDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.repos.ArtistRepo
import wtf.ndu.vibin.repos.TagRepo

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

        postP("/api/metadata/create", PermissionType.MANAGE_TRACKS) {
            val request = call.receive<CreateMetadataDto>()

            val artists = request.artistNames.map { ArtistRepo.getOrCreateArtist(it) }
            val tags = request.tagNames.map { TagRepo.getOrCreateTag(it) }
            val album = request.albumName?.let { AlbumRepo.getOrCreateAlbum(request.albumName) }

            call.respond(mapOf(
                "artists" to ArtistRepo.toDto(artists),
                "tags" to TagRepo.toDto(tags),
                "album" to album?.let { AlbumRepo.toDto(it) }
            ))
        }
    }
}
