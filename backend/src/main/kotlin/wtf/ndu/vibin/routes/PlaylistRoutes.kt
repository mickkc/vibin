package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.PlaylistEditDto
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

            val total = PlaylistRepo.count(userId)
            val playlists = PlaylistRepo.getAll(page, pageSize, userId)

            call.respond(PaginatedDto(
                    items = PlaylistRepo.toDto(playlists),
                    total = total.toInt(),
                    pageSize = pageSize,
                    currentPage = page
                )
            )
        }

        postP("/api/playlists", PermissionType.MANAGE_PLAYLISTS) {

            val userId = call.getUserId() ?: return@postP call.unauthorized()
            val editDto = call.receive<PlaylistEditDto>()

            if ((editDto.isPublic == true && !call.hasPermissions(PermissionType.CREATE_PUBLIC_PLAYLISTS) ||
                (editDto.isPublic == false && !call.hasPermissions(PermissionType.CREATE_PRIVATE_PLAYLISTS))))
                return@postP call.forbidden()

            val created = PlaylistRepo.createOrUpdatePlaylist(userId, editDto, null)!!

            call.respond(PlaylistRepo.toDto(created))
        }

        putP("/api/playlists/{playlistId}", PermissionType.MANAGE_PLAYLISTS) {
            val userId = call.getUserId() ?: return@putP call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@putP call.missingParameter("playlistId")
            val editDto = call.receive<PlaylistEditDto>()

            if ((editDto.isPublic == true && !call.hasPermissions(PermissionType.CREATE_PUBLIC_PLAYLISTS) ||
                (editDto.isPublic == false && !call.hasPermissions(PermissionType.CREATE_PRIVATE_PLAYLISTS))))
                return@putP call.forbidden()

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@putP call.notFound()

            val updated = PlaylistRepo.createOrUpdatePlaylist(userId, editDto, playlist.id.value) ?: return@putP call.notFound()

            call.respond(PlaylistRepo.toDto(updated))
        }

        deleteP("/api/playlists/{playlistId}", PermissionType.DELETE_OWN_PLAYLISTS) {
            val userId = call.getUserId() ?: return@deleteP call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("playlistId")

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@deleteP call.notFound()

            PlaylistRepo.deletePlaylist(playlist.id.value)

            call.respond(mapOf("status" to "success"))
        }
    }
}
