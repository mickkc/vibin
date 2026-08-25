package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.artists.ArtistEditData
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.ArtistRepo

fun Application.configureArtistRoutes() = routing {

    authenticate("tokenAuth") {
        getP("/api/artists", PermissionType.VIEW_ARTISTS) {

            val params = call.getPaginatedSearchParams()
            val userId = call.getUserId() ?: unauthorized()

            val (artists, total) = ArtistRepo.getAll(params, userId)

            call.respond(PaginatedDto(
                items = ArtistRepo.toDto(artists),
                total = total.toInt(),
                currentPage = params.page,
                pageSize = params.pageSize
            ))
        }

        getP("/api/artists/{id}", PermissionType.VIEW_ARTISTS) {
            val id = call.getLongParameter("id")
            val artist = ArtistRepo.getById(id) ?: notFound()
            call.respond(ArtistRepo.toDto(artist))
        }

        getP("/api/artists/ids", PermissionType.VIEW_ARTISTS) {
            val idsParam = call.getStringParameter("ids")
            val ids = idsParam.split(",").mapNotNull { it.toLongOrNull() }
            val artists = ArtistRepo.getByIds(ids)
            call.respond(ArtistRepo.toDto(artists))
        }

        postP("/api/artists", PermissionType.MANAGE_ARTISTS) {
            val data = call.receive<ArtistEditData>()
            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(null, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (e400: IllegalStateException) {
                missingParameter(e400.message!!)
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }

        putP("/api/artists/{id}", PermissionType.MANAGE_ARTISTS) {
            val id = call.getLongParameter("id")
            val data = call.receive<ArtistEditData>()
            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(id, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (_: NotFoundException) {
                notFound()
            }
        }

        deleteP("/api/artists/{id}", PermissionType.DELETE_ARTISTS) {
            val id = call.getLongParameter("id")
            val success = ArtistRepo.deleteArtist(id)

            if (!success) {
                notFound()
            }

            call.success()
        }

        getP("/api/artists/{artistId}/image", PermissionType.VIEW_ARTISTS) {
            val artistId = call.getLongParameter("artistId")
            val quality = call.getIntOrDefault("quality", 0)
            val imageId = ArtistRepo.getImageId(artistId)

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=artist&quality=$quality")
        }

        getP("/api/artists/autocomplete", PermissionType.VIEW_ARTISTS) {
            val query = call.getStringParameter("query")
            val limit = call.getIntOrDefault("limit", 10)

            val artistNames = ArtistRepo.autocomplete(query, limit)
            call.respond(artistNames)
        }
    }
}