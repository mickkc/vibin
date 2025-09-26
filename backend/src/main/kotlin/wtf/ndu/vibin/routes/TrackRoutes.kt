package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.db.ListenType
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.ListenRepo
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.SessionRepo
import wtf.ndu.vibin.repos.TrackRepo
import wtf.ndu.vibin.settings.PageSize
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.utils.ImageUtils
import wtf.ndu.vibin.utils.PathUtils

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

        // Get related tracks
        getP("/api/tracks/{trackId}/related", PermissionType.VIEW_TRACKS) {
            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@getP call.missingParameter("trackId")
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
            val related = TrackRepo.getRelated(track, limit)
            call.respond(TrackRepo.toMinimalDto(related))
        }

        // Search tracks
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

        getP("/api/tracks/random", PermissionType.VIEW_TRACKS) {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 1
            val track = TrackRepo.getRandom(limit)
            call.respond(TrackRepo.toMinimalDto(track))
        }
    }

    // Get track cover image
    // Not using authentication to allow fetching covers with the token as a query parameter instead of a header
    getP("/api/tracks/{trackId}/cover") {
        val mediaToken = call.parameters["mediaToken"]
        val userId = call.getUserId() ?: mediaToken?.let { SessionRepo.getUserFromMediaToken(mediaToken)?.id?.value } ?: return@getP call.unauthorized()

        if (!PermissionRepo.hasPermissions(userId, listOf(PermissionType.VIEW_TRACKS))) {
            return@getP call.forbidden(PermissionType.VIEW_TRACKS)
        }

        val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@getP call.missingParameter("trackId")
        val quality = call.request.queryParameters["quality"] ?: "original"
        val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
        val cover = TrackRepo.getCover(track)

        call.respondFile(ImageUtils.getFileOrDefault(cover, quality, "track") )
    }

    // TODO: Move into authenticated block when headers are fixed on Web
    getP("/api/tracks/{trackId}/stream") {
        val mediaToken = call.parameters["mediaToken"]
        val userId = call.getUserId() ?: mediaToken?.let { SessionRepo.getUserFromMediaToken(mediaToken)?.id?.value } ?: return@getP call.unauthorized()

        if (!PermissionRepo.hasPermissions(userId, listOf(PermissionType.STREAM_TRACKS))) {
            return@getP call.forbidden(PermissionType.STREAM_TRACKS)
        }

        val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@getP call.missingParameter("trackId")
        val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()

        val audioFile = PathUtils.getTrackFileFromPath(track.path)
        if (!audioFile.exists()) {
            return@getP call.notFound()
        }

        ListenRepo.listenedTo(userId, track.id.value, ListenType.TRACK)

        call.respondFile(audioFile)
    }
}