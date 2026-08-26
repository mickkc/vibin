package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.SortDirection
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.dto.tracks.TrackSortingCriteria
import de.mickkc.vibin.dto.tracks.batch.BatchEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.BatchEditRepo
import de.mickkc.vibin.repos.ColorSchemeRepo
import de.mickkc.vibin.repos.LyricsRepo
import de.mickkc.vibin.repos.TrackRepo
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
            val page = call.getIntOrDefault("page", 1)
            val pageSize = call.getIntOrDefault("pageSize", 50)
            val userId = call.getUserId() ?: unauthorized()

            val sortCriteria = call.getEnum("sort", TrackSortingCriteria.TITLE)
            val sortDirection = call.getEnum("dir", SortDirection.ASC)

            val (tracks, total) = TrackRepo.getAll(page, pageSize, sortingCriteria = sortCriteria, sortingDirection = sortDirection, userId = userId)

            call.respond(PaginatedDto(
                items = TrackRepo.toMinimalDto(tracks),
                total = total.toInt(),
                pageSize = pageSize,
                currentPage = page
            ))
        }

        // Get track by ID
        getP("/api/tracks/{trackId}", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()
            call.respond(TrackRepo.toDto(track))
        }

        // Get tracks by artist ID
        getP("/api/tracks/artists/{artistId}", PermissionType.VIEW_TRACKS) {
            val artistId = call.getLongParameter("artistId")
            val userId = call.getUserId() ?: unauthorized()

            val tracks = TrackRepo.getByArtistId(artistId, userId)
            call.respond(TrackRepo.toDto(tracks))
        }

        // Edit track details
        putP("/api/tracks/{trackId}", PermissionType.MANAGE_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val editDto = call.receive<TrackEditDto>()
            editDto.validate()

            val updated = TrackRepo.update(trackId, editDto) ?: notFound()
            call.respond(TrackRepo.toDto(updated))
        }

        // Delete track
        deleteP("/api/tracks/{trackId}", PermissionType.DELETE_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()

            TrackRepo.delete(track)
            call.success()
        }

        // Get related tracks
        getP("/api/tracks/{trackId}/similar", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val limit = call.getIntOrDefault("limit", 10)
            val userId = call.getUserId()

            val track = TrackRepo.getById(trackId) ?: notFound()
            val related = TrackRepo.getRelated(track, limit, userId)
            call.respond(TrackRepo.toMinimalDto(related))
        }

        // Search tracks
        getP("/api/tracks/search", PermissionType.VIEW_TRACKS) {

            val params = call.getPaginatedSearchParams()
            val advanced = call.getBooleanOrDefault("advanced", false)
            val userId = call.getUserId() ?: unauthorized()

            val sortCriteria = call.getEnum("sort", TrackSortingCriteria.TITLE)
            val sortDirection = call.getEnum("dir", SortDirection.ASC)

            val (results, count) = TrackRepo.getSearched(params, advanced, sortingCriteria = sortCriteria, sortDirection = sortDirection, userId = userId)

            call.respond(PaginatedDto(
                items = TrackRepo.toMinimalDto(results),
                total = count.toInt(),
                pageSize = params.pageSize,
                currentPage = params.page
            ))
        }

        getP("/api/tracks/random", PermissionType.VIEW_TRACKS) {
            val limit = call.getIntOrDefault("limit", 1)
            val userId = call.getUserId() ?: unauthorized()

            val track = TrackRepo.getRandom(limit, userId)

            call.respond(TrackRepo.toMinimalDto(track))
        }

        getP("/api/tracks/newest", PermissionType.VIEW_TRACKS) {
            val limit = call.getIntOrDefault("limit", 10)
            val userId = call.getUserId() ?: unauthorized()

            val tracks = TrackRepo.getNewest(limit, userId)
            call.respond(TrackRepo.toMinimalDto(tracks))
        }


        getP("/api/tracks/{trackId}/lyrics", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")

            val track = TrackRepo.getById(trackId) ?: notFound()
            val lyrics = LyricsRepo.getLyrics(track)?.content
            val colorScheme = TrackRepo.getColorScheme(track)

            call.respond(mapOf(
                "lyrics" to lyrics,
                "colorScheme" to colorScheme?.let { ColorSchemeRepo.toDto(it) }
            ))
        }

        getP("/api/tracks/{trackId}/lyrics/check", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val hasLyrics = LyricsRepo.hasLyrics(trackId)
            call.success(hasLyrics)
        }

        getP("/api/tracks/{trackId}/download", PermissionType.DOWNLOAD_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()

            val audioFile = PathUtils.getTrackFileFromPath(track.path)
            if (!audioFile.exists()) {
                notFound()
            }

            call.respondFile(audioFile)
        }

        getP("/api/tracks/{trackId}/cover", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val quality = call.getIntOrDefault("quality", 0)
            val imageId = TrackRepo.getCoverId(trackId)

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=track&quality=$quality")
        }

        postP("/api/tracks/edit/batch", PermissionType.MANAGE_TRACKS) {
            val userId = call.getUserId()
                ?: unauthorized()

            val editDto = call.receive<BatchEditDto>()

            val result = BatchEditRepo.batchEdit(editDto, userId)

            call.respond(result)
        }
    }
}