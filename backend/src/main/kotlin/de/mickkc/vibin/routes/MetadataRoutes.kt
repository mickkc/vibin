package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.CreateMetadataDto
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.TagRepo

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
            val album = request.albumName?.let { AlbumRepo.getOrCreateAlbum(request.albumName, request.artistNames.firstOrNull()) }

            call.respond(mapOf(
                "artists" to ArtistRepo.toDto(artists),
                "tags" to TagRepo.toDto(tags),
                "album" to album?.let { AlbumRepo.toDto(it) }
            ))
        }
    }
}
