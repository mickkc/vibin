package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
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

fun Application.configureArtistRoutes() = routing {

    authenticate("tokenAuth") {
        getP("/api/artists", PermissionType.VIEW_ARTISTS) {
            val page = call.parameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.parameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

            val total = ArtistRepo.count()
            val artists = ArtistRepo.getAll(page, pageSize)
            call.respond(PaginatedDto(
                items = ArtistRepo.toDto(artists),
                total = total.toInt(),
                currentPage = page,
                pageSize = pageSize
            ))
        }

        postP("/api/artists", PermissionType.MANAGE_ARTISTS) {
            val data = call.receive<ArtistEditData>()
            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(null, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (e400: IllegalStateException) {
                call.badRequest(e400.message ?: "Bad Request")
            }
            catch (e404: NotFoundException) {
                call.notFound(e404.message ?: "Not Found")
            }
        }

        putP("/api/artists/{id}", PermissionType.MANAGE_ARTISTS) {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                return@putP call.missingParameter("id")
            }
            val data = call.receive<ArtistEditData>()
            try {
                val updatedArtist = ArtistRepo.updateOrCreateArtist(id, data)
                call.respond(ArtistRepo.toDto(updatedArtist))
            }
            catch (e400: IllegalStateException) {
                call.badRequest(e400.message ?: "Bad Request")
            }
            catch (e404: NotFoundException) {
                call.notFound(e404.message ?: "Not Found")
            }
        }

        deleteP("/api/artists/{id}", PermissionType.DELETE_ARTISTS) {
            val id = call.parameters["id"]?.toLongOrNull()
            if (id == null) {
                return@deleteP call.missingParameter("id")
            }
            val success = ArtistRepo.deleteArtist(id)

            if (!success) {
                return@deleteP call.notFound()
            }

            call.respond(mapOf(
                "success" to true
            ))
        }
    }
}