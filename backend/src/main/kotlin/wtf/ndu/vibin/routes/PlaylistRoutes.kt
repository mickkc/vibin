package wtf.ndu.vibin.routes

import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.playlists.PlaylistEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PlaylistRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

fun Application.configurePlaylistRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/playlists", PermissionType.VIEW_PLAYLISTS) {

            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val page = call.request.queryParameters["p"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)
            val query = call.request.queryParameters["query"]
            val onlyOwn = call.request.queryParameters["onlyOwn"]?.toBoolean() ?: false

            // Get the playlists for the requested page
            val (playlists, total) = PlaylistRepo.getAll(page, pageSize, userId, query ?: "", onlyOwn)

            call.respond(PaginatedDto(
                items = PlaylistRepo.toDto(playlists),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        getP("/api/playlists/{playlistId}", PermissionType.VIEW_PLAYLISTS, PermissionType.VIEW_TRACKS) {
            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@getP call.missingParameter("playlistId")

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@getP call.notFound()

            val tracks = PlaylistRepo.getTracksWithSource(playlist)
            call.respond(PlaylistRepo.toDataDto(playlist, tracks))
        }

        suspend fun getValidatedEditDto(call: RoutingCall): PlaylistEditDto? {
            val editDto = call.receive<PlaylistEditDto>()

            if (editDto.name.isBlank()) {
                call.missingParameter("name")
                return null
            }

            // Check permissions for public/private playlists
            if ((editDto.isPublic == true && !call.hasPermissions(PermissionType.CREATE_PUBLIC_PLAYLISTS) ||
                (editDto.isPublic == false && !call.hasPermissions(PermissionType.CREATE_PRIVATE_PLAYLISTS)))) {
                call.forbidden(if (editDto.isPublic) PermissionType.CREATE_PUBLIC_PLAYLISTS else PermissionType.CREATE_PRIVATE_PLAYLISTS)
                return null
            }

            return editDto
        }

        postP("/api/playlists", PermissionType.MANAGE_PLAYLISTS) {

            val user = call.getUser() ?: return@postP call.unauthorized()
            val editDto = getValidatedEditDto(call) ?: return@postP

            // Create the playlist
            val created = PlaylistRepo.createOrUpdatePlaylist(user, editDto, null)!!

            call.respond(PlaylistRepo.toDto(created))
        }

        putP("/api/playlists/{playlistId}", PermissionType.MANAGE_PLAYLISTS) {
            val user = call.getUser() ?: return@putP call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@putP call.missingParameter("playlistId")
            val editDto = getValidatedEditDto(call) ?: return@putP

            // Get the playlist to check ownership
            val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, user.id.value, PermissionType.EDIT_COLLABORATIVE_PLAYLISTS)
                ?: return@putP call.notFound()

            // Update the playlist
            val updated = PlaylistRepo.createOrUpdatePlaylist(user, editDto, playlist.id.value) ?: return@putP call.notFound()

            call.respond(PlaylistRepo.toDto(updated))
        }

        deleteP("/api/playlists/{playlistId}", PermissionType.DELETE_OWN_PLAYLISTS) {
            val userId = call.getUserId() ?: return@deleteP call.unauthorized()
            val playlistId =
                call.parameters["playlistId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("playlistId")

            // Get the playlist to check ownership
            val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, userId, PermissionType.DELETE_COLLABORATIVE_PLAYLISTS)
                ?: return@deleteP call.notFound()

            PlaylistRepo.deletePlaylist(playlist.id.value)

            call.success()
        }

        getP("/api/playlists/{playlistId}/image", PermissionType.VIEW_PLAYLISTS) {
            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@getP call.missingParameter("playlistId")
            val quality = call.request.queryParameters["quality"] ?: "original"

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@getP call.notFound()

            val imageBytes = PlaylistRepo.getCoverImageBytes(playlist, quality)

            call.respondBytes(imageBytes, contentType = ContentType.Image.JPEG)
        }
    }
}
