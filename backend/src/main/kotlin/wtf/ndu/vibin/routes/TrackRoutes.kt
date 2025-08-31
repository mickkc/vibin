package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.TrackEditDto
import wtf.ndu.vibin.repos.TrackRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings

fun Application.configureTrackRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/tracks") {
            val page = call.request.queryParameters["p"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: Settings.get(PageSize)

            val total = TrackRepo.count()
            val tracks = TrackRepo.getAll(page, pageSize)

            call.respond(PaginatedDto(
                items = TrackRepo.toDto(tracks),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        post("/api/tracks/{trackId}/edit") {
            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@post call.missingParameter("trackId")
            val editDto = call.receive<TrackEditDto>()

            val updated = TrackRepo.update(trackId, editDto) ?: return@post call.notFound()
            call.respond(TrackRepo.toDto(updated))
        }
    }
}