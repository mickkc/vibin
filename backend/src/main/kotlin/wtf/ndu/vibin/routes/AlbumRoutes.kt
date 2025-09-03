package wtf.ndu.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

fun Application.configureAlbumRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/albums") {
            val page = call.request.queryParameters["p"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

            val total = AlbumRepo.count()
            val albums = AlbumRepo.getAll(page, pageSize)

            call.respond(PaginatedDto(
                items = AlbumRepo.toDto(albums),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        get("/api/albums/{albumId}") {
            val albumId = call.parameters["albumId"]?.toLongOrNull() ?: return@get call.missingParameter("albumId")
            val album = AlbumRepo.getById(albumId)

            if (album == null) {
                return@get call.notFound()
            }

            call.respond(AlbumRepo.toDataDto(album))
        }
    }
}