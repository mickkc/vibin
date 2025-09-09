package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.TrackRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

fun Application.configureTrackRoutes() = routing {

    authenticate("tokenAuth") {

        // Get all tracks (paginated)
        getP("/api/tracks", PermissionType.VIEW_TRACKS) {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

            val (tracks, total) = TrackRepo.getAll(page, pageSize)

            call.respond(PaginatedDto(
                items = TrackRepo.toMinimalDto(tracks),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        // Get track by ID
        getP("/api/tracks/{trackId}", PermissionType.VIEW_TRACKS) {
            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@getP call.missingParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
            call.respond(TrackRepo.toDto(track))
        }

        // Edit track details
        putP("/api/tracks/{trackId}", PermissionType.MANAGE_TRACKS) {
            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@putP call.missingParameter("trackId")
            val editDto = call.receive<TrackEditDto>()

            val updated = TrackRepo.update(trackId, editDto) ?: return@putP call.notFound()
            call.respond(TrackRepo.toDto(updated))
        }

        // Delete track
        deleteP("/api/tracks/{trackId}", PermissionType.DELETE_TRACKS) {
            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("trackId")

            val track = TrackRepo.getById(trackId) ?: return@deleteP call.notFound()
            TrackRepo.delete(track)
            call.success()
        }

        getP("/api/tracks/search", PermissionType.VIEW_TRACKS) {
            val query = call.request.queryParameters["query"] ?: return@getP call.missingParameter("query")
            val advanced = call.request.queryParameters["advanced"]?.toBooleanStrictOrNull() ?: false
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

            val (results, count) = TrackRepo.getSearched(query, advanced, page, pageSize)

            call.respond(PaginatedDto(
                items = TrackRepo.toMinimalDto(results),
                total = count.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }
    }
}