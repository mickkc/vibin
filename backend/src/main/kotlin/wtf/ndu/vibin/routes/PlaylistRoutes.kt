package wtf.ndu.vibin.routes

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

            // Get the playlists for the requested page
            val (playlists, total) = PlaylistRepo.getAll(page, pageSize, userId)

            call.respond(PaginatedDto(
                    items = PlaylistRepo.toDto(playlists),
                    total = total.toInt(),
                    pageSize = pageSize,
                    currentPage = page
                )
            )
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
                call.forbidden()
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
            val playlist = PlaylistRepo.getByIdCollaborative(playlistId, user.id.value) ?: return@putP call.notFound()

            // Prevent editing others' playlists unless having the permission
            if (!PlaylistRepo.checkOwnership(playlist, user.id.value) && !call.hasPermissions(PermissionType.EDIT_COLLABORATIVE_PLAYLISTS))
                return@putP call.forbidden()

            // Update the playlist
            val updated = PlaylistRepo.createOrUpdatePlaylist(user, editDto, playlist.id.value) ?: return@putP call.notFound()

            call.respond(PlaylistRepo.toDto(updated))
        }

        deleteP("/api/playlists/{playlistId}", PermissionType.DELETE_OWN_PLAYLISTS) {
            val userId = call.getUserId() ?: return@deleteP call.unauthorized()
            val playlistId =
                call.parameters["playlistId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("playlistId")

            // Get the playlist to check ownership
            val playlist = PlaylistRepo.getByIdCollaborative(playlistId, userId) ?: return@deleteP call.notFound()

            // Prevent deleting others' playlists unless having the permission
            if (!PlaylistRepo.checkOwnership(playlist, userId) && !call.hasPermissions(PermissionType.DELETE_COLLABORATIVE_PLAYLISTS))
                return@deleteP call.forbidden()

            PlaylistRepo.deletePlaylist(playlist.id.value)

            call.respond(mapOf("success" to true))
        }
    }
}
