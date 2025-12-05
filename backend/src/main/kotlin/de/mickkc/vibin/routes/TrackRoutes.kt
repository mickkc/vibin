package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.*
import de.mickkc.vibin.utils.ImageUtils
import de.mickkc.vibin.utils.PathUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureTrackRoutes() = routing {

    authenticate("tokenAuth") {

        // Get all tracks (paginated)
        getP("/api/tracks", PermissionType.VIEW_TRACKS) {
            val page = call.getIntOrDefault("page", 1) ?: return@getP
            val pageSize = call.getIntOrDefault("pageSize", 50) ?: return@getP
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val (tracks, total) = TrackRepo.getAll(page, pageSize, userId)

            call.respond(PaginatedDto(
                items = TrackRepo.toMinimalDto(tracks),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        // Get track by ID
        getP("/api/tracks/{trackId}", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@getP
            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
            call.respond(TrackRepo.toDto(track))
        }

        // Get tracks by artist ID
        getP("/api/tracks/artists/{artistId}", PermissionType.VIEW_TRACKS) {
            val artistId = call.getLongParameter("artistId") ?: return@getP
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val tracks = TrackRepo.getByArtistId(artistId, userId)
            call.respond(TrackRepo.toDto(tracks))
        }

        // Edit track details
        putP("/api/tracks/{trackId}", PermissionType.MANAGE_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@putP
            val editDto = call.receive<TrackEditDto>()

            val updated = TrackRepo.update(trackId, editDto) ?: return@putP call.notFound()
            call.respond(TrackRepo.toDto(updated))
        }

        // Delete track
        deleteP("/api/tracks/{trackId}", PermissionType.DELETE_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@deleteP
            val track = TrackRepo.getById(trackId) ?: return@deleteP call.notFound()

            TrackRepo.delete(track)
            call.success()
        }

        // Get related tracks
        getP("/api/tracks/{trackId}/similar", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@getP
            val limit = call.getIntOrDefault("limit", 10) ?: return@getP

            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
            val related = TrackRepo.getRelated(track, limit)
            call.respond(TrackRepo.toMinimalDto(related))
        }

        // Search tracks
        getP("/api/tracks/search", PermissionType.VIEW_TRACKS) {

            val params = call.getPaginatedSearchParams() ?: return@getP
            val advanced = call.getBooleanOrDefault("advanced", false) ?: return@getP
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val (results, count) = TrackRepo.getSearched(params, advanced, userId)

            call.respond(PaginatedDto(
                items = TrackRepo.toMinimalDto(results),
                total = count.toInt(),
                pageSize = params.pageSize,
                currentPage = params.page
            ))
        }

        getP("/api/tracks/random", PermissionType.VIEW_TRACKS) {
            val limit = call.getIntOrDefault("limit", 1) ?: return@getP
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val track = TrackRepo.getRandom(limit, userId)

            call.respond(TrackRepo.toMinimalDto(track))
        }

        getP("/api/tracks/newest", PermissionType.VIEW_TRACKS) {
            val limit = call.getIntOrDefault("limit", 10) ?: return@getP
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val tracks = TrackRepo.getNewest(limit, userId)
            call.respond(TrackRepo.toMinimalDto(tracks))
        }


        getP("/api/tracks/{trackId}/lyrics", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@getP

            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
            val lyrics = LyricsRepo.getLyrics(track)?.content
            val colorScheme = TrackRepo.getColorScheme(track)

            call.respond(mapOf(
                "lyrics" to lyrics,
                "colorScheme" to colorScheme?.let { ColorSchemeRepo.toDto(it) }
            ))
        }

        getP("/api/tracks/{trackId}/lyrics/check", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@getP
            val hasLyrics = LyricsRepo.hasLyrics(trackId)
            call.success(hasLyrics)
        }

        getP("/api/tracks/{trackId}/download", PermissionType.DOWNLOAD_TRACKS) {
            val trackId = call.getLongParameter("trackId") ?: return@getP
            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()

            val audioFile = PathUtils.getTrackFileFromPath(track.path)
            if (!audioFile.exists()) {
                return@getP call.notFound()
            }

            call.respondFile(audioFile)
        }
    }

    fun getUserIdFromCall(call: ApplicationCall): Long? {
        return call.request.headers["Authorization"]?.removePrefix("Bearer ")?.let { SessionRepo.validateAndUpdateToken(it) }
            ?: call.parameters["mediaToken"]?.let { SessionRepo.getUserFromMediaToken(it)?.id?.value }
    }

    // Get track cover image
    // Not using authentication to allow fetching covers with the token as a query parameter instead of a header
    getP("/api/tracks/{trackId}/cover") {
        val userId = getUserIdFromCall(call) ?: return@getP call.unauthorized()

        if (!PermissionRepo.hasPermissions(userId, listOf(PermissionType.VIEW_TRACKS))) {
            return@getP call.forbidden(PermissionType.VIEW_TRACKS)
        }

        val trackId = call.getLongParameter("trackId") ?: return@getP
        val quality = call.getIntOrDefault("quality", 0) ?: return@getP
        val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
        val cover = TrackRepo.getCover(track)

        val file = ImageUtils.getFileOrDefault(cover, quality, "track") ?: return@getP call.notFound()

        call.respondFile(file)
    }

    // TODO: Move into authenticated block when headers are fixed on Web
    getP("/api/tracks/{trackId}/stream") {
        val userId = getUserIdFromCall(call) ?: return@getP call.unauthorized()

        if (!PermissionRepo.hasPermissions(userId, listOf(PermissionType.STREAM_TRACKS))) {
            return@getP call.forbidden(PermissionType.STREAM_TRACKS)
        }

        val trackId = call.getLongParameter("trackId") ?: return@getP
        val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()

        val audioFile = PathUtils.getTrackFileFromPath(track.path)
        if (!audioFile.exists()) {
            return@getP call.notFound()
        }

        call.respondFile(audioFile)
    }
}