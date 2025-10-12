package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.artists.ArtistEditData
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.ArtistRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.utils.ImageUtils

fun Application.configureArtistRoutes() = routing {

    authenticate("tokenAuth") {
        getP("/api/artists", PermissionType.VIEW_ARTISTS) {
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)
            val query = call.request.queryParameters["query"]

            val (artists, total) = ArtistRepo.getAll(page, pageSize, query ?: "")

            call.respond(PaginatedDto(
                items = ArtistRepo.toDto(artists),
                total = total.toInt(),
                currentPage = page,
                pageSize = pageSize
            ))
        }

        getP("/api/artists/{id}", PermissionType.VIEW_ARTISTS) {
            val id = call.parameters["id"]?.toLongOrNull() ?: return@getP call.missingParameter("id")
            val artist = ArtistRepo.getById(id) ?: return@getP call.notFound()
            call.respond(ArtistRepo.toDto(artist))
        }

        postP("/api/artists", PermissionType.MANAGE_ARTISTS) {
            val data = call.receive<ArtistEditData>()
            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(null, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (e400: IllegalStateException) {
                call.missingParameter(e400.message!!)
            }
            catch (_: NotFoundException) {
                call.notFound()
            }
        }

        putP("/api/artists/{id}", PermissionType.MANAGE_ARTISTS) {
            val id = call.parameters["id"]?.toLongOrNull() ?: return@putP call.missingParameter("id")
            val data = call.receive<ArtistEditData>()
            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(id, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (_: NotFoundException) {
                call.notFound()
            }
        }

        deleteP("/api/artists/{id}", PermissionType.DELETE_ARTISTS) {
            val id = call.parameters["id"]?.toLongOrNull() ?: return@deleteP call.missingParameter("id")
            val success = ArtistRepo.deleteArtist(id)

            if (!success) {
                return@deleteP call.notFound()
            }

            call.success()
        }

        getP("/api/artists/{artistId}/image", PermissionType.VIEW_ARTISTS) {
            val artistId = call.parameters["artistId"]?.toLongOrNull() ?: return@getP call.missingParameter("artistId")
            val quality = call.request.queryParameters["quality"] ?: "original"
            val artist = ArtistRepo.getById(artistId) ?: return@getP call.notFound()
            val image = ArtistRepo.getImage(artist)

            call.respondFile(ImageUtils.getFileOrDefault(image, quality, "artist"))
        }

        getP("/api/artists/autocomplete", PermissionType.VIEW_ARTISTS) {
            val query = call.request.queryParameters["query"] ?: return@getP call.missingParameter("query")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            val artistNames = ArtistRepo.autocomplete(query, limit)
            call.respond(artistNames)
        }
    }
}