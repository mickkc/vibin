package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

fun Application.configureAlbumRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/albums", PermissionType.VIEW_ALBUMS) {

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

            val (albums, total) = AlbumRepo.getAll(page, pageSize)

            call.respond(PaginatedDto(
                items = AlbumRepo.toDto(albums),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        getP("/api/albums/{albumId}", PermissionType.VIEW_ALBUMS) {

            val albumId = call.parameters["albumId"]?.toLongOrNull() ?: return@getP call.missingParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: return@getP call.notFound()

            call.respond(AlbumRepo.toDataDto(album))
        }
    }
}