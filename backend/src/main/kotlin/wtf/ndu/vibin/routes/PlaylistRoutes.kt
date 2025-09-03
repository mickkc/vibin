package wtf.ndu.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.PlaylistEditDto
import wtf.ndu.vibin.repos.PlaylistRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

fun Application.configurePlaylistRoutes() = routing {
    authenticate("tokenAuth") {

        get("/api/playlists") {
            val userId = call.getUserId() ?: return@get call.unauthorized()
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

        post("/api/playlists") {
            val userId = call.getUserId() ?: return@post call.unauthorized()
            val editDto = call.receive<PlaylistEditDto>()

            val created = PlaylistRepo.createOrUpdatePlaylist(userId, editDto, null)!!

            call.respond(PlaylistRepo.toDto(created))
        }

        put("/api/playlists/{playlistId}") {
            val userId = call.getUserId() ?: return@put call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@put call.missingParameter("playlistId")
            val editDto = call.receive<PlaylistEditDto>()

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@put call.notFound()

            val updated = PlaylistRepo.createOrUpdatePlaylist(userId, editDto, playlist.id.value) ?: return@put call.notFound()

            call.respond(PlaylistRepo.toDto(updated))
        }

        delete("/api/playlists/{playlistId}") {
            val userId = call.getUserId() ?: return@delete call.unauthorized()
            val playlistId = call.parameters["playlistId"]?.toLongOrNull() ?: return@delete call.missingParameter("playlistId")

            val playlist = PlaylistRepo.getById(playlistId, userId) ?: return@delete call.notFound()

            PlaylistRepo.deletePlaylist(playlist.id.value)

            call.respond(mapOf("status" to "success"))
        }
    }
}
