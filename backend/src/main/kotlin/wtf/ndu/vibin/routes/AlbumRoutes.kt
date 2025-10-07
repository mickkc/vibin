package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.albums.AlbumEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.utils.ImageUtils

fun Application.configureAlbumRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/albums", PermissionType.VIEW_ALBUMS) {

            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)
            val query = call.request.queryParameters["query"]
            val showSingles = call.request.queryParameters["showSingles"]?.toBoolean() ?: true

            val (albums, total) = AlbumRepo.getAll(page, pageSize, query ?: "", showSingles = showSingles)

            call.respond(PaginatedDto(
                items = AlbumRepo.toDto(albums),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        getP("/api/albums/autocomplete", PermissionType.VIEW_ALBUMS) {

            val query = call.request.queryParameters["query"] ?: return@getP call.missingParameter("query")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10

            val albumNames = AlbumRepo.autocomplete(query, limit)

            call.respond(albumNames)
        }

        getP("/api/albums/{albumId}", PermissionType.VIEW_ALBUMS) {

            val albumId = call.parameters["albumId"]?.toLongOrNull() ?: return@getP call.missingParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: return@getP call.notFound()

            call.respond(AlbumRepo.toDataDto(album))
        }

        putP("/api/albums/{albumId}", PermissionType.MANAGE_ALBUMS) {

            val albumId = call.parameters["albumId"]?.toLongOrNull() ?: return@putP call.missingParameter("albumId")
            val editDto = call.receive<AlbumEditDto>()

            val updated = AlbumRepo.update(albumId, editDto) ?: return@putP call.notFound()
            call.respond(AlbumRepo.toDto(updated))
        }

        getP("/api/albums/{albumId}/cover", PermissionType.VIEW_ALBUMS) {
            val albumId = call.parameters["albumId"]?.toLongOrNull() ?: return@getP call.missingParameter("albumId")
            val album = AlbumRepo.getById(albumId) ?: return@getP call.notFound()
            val quality = call.request.queryParameters["quality"] ?: "original"
            val cover = AlbumRepo.getAlbumCover(album)

            call.respondFile(ImageUtils.getFileOrDefault(cover, quality, "album"))
        }
    }
}