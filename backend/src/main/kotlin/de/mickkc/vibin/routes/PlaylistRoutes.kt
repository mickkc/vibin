package de.mickkc.vibin.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.playlists.PlaylistEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PlaylistRepo

fun Application.configurePlaylistRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/playlists", PermissionType.VIEW_PLAYLISTS) {

            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val params = call.getPaginatedSearchParams() ?: return@getP
            val onlyOwn = call.getBooleanOrDefault("onlyOwn", false) ?: return@getP

            // Get the playlists for the requested page
            val (playlists, total) = PlaylistRepo.getAll(params, userId, onlyOwn)

            call.respond(PaginatedDto(
                items = PlaylistRepo.toDto(playlists),
                total = total.toInt(),
                pageSize = params.pageSize,
                currentPage = params.page
            ))
        }

        getP("/api/playlists/{playlistId}", PermissionType.VIEW_PLAYLISTS, PermissionType.VIEW_TRACKS) {
            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val playlistId = call.getLongParameter("playlistId") ?: return@getP

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@getP call.notFound()

            call.respond(PlaylistRepo.toDataDto(playlist, userId))
        }

        getP("/api/playlists/users/{userId}", PermissionType.VIEW_PLAYLISTS, PermissionType.VIEW_USERS) {
            val requestingUserId = call.getUserId() ?: return@getP call.unauthorized()
            val userId = call.getLongParameter("userId") ?: return@getP

            val playlists = PlaylistRepo.getAllForUser(userId, userId == requestingUserId)

            call.respond(PlaylistRepo.toDto(playlists))
        }

        getP("/api/playlists/random", PermissionType.VIEW_PLAYLISTS) {
            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val limit = call.getIntOrDefault("limit", 1) ?: return@getP

            val playlists = PlaylistRepo.getRandom(limit, userId)

            call.respond(PlaylistRepo.toDto(playlists))
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
            val playlistId = call.getLongParameter("playlistId") ?: return@putP
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
            val playlistId = call.getLongParameter("playlistId") ?: return@deleteP

            // Get the playlist to check ownership
            val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, userId, PermissionType.DELETE_COLLABORATIVE_PLAYLISTS)
                ?: return@deleteP call.notFound()

            PlaylistRepo.deletePlaylist(playlist.id.value)

            call.success()
        }

        getP("/api/playlists/{playlistId}/image", PermissionType.VIEW_PLAYLISTS) {
            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val playlistId = call.getLongParameter("playlistId") ?: return@getP
            val quality = call.getIntOrDefault("quality", 0) ?: return@getP

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@getP call.notFound()

            val imageBytes = PlaylistRepo.getCoverImageBytes(playlist, quality) ?: return@getP call.notFound()

            call.respondBytes(imageBytes, contentType = ContentType.Image.JPEG)
        }
    }
}
