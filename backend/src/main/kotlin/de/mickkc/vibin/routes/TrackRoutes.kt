package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.LyricsDto
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.SortDirection
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackDto
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.dto.tracks.TrackSortingCriteria
import de.mickkc.vibin.dto.tracks.batch.BatchEditDto
import de.mickkc.vibin.dto.tracks.batch.BatchEditResultDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.BatchEditRepo
import de.mickkc.vibin.repos.ColorSchemeRepo
import de.mickkc.vibin.repos.LyricsRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.utils.PathUtils
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*

@OptIn(ExperimentalKtorApi::class)
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
        }.describe {
            operationId = "getAllTracks"
            summary = "Get all tracks"
            tag("Tracks")
            parameters {
                q("page", 1, required = false)
                q("pageSize", 50, required = false)
                q("sort", TrackSortingCriteria.TITLE, required = false)
                q("dir", SortDirection.ASC, required = false)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<PaginatedDto<MinimalTrackDto>>("A paginated list of tracks")
            }
        }

        // Get track by ID
        getP("/api/tracks/{trackId}", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()
            call.respond(TrackRepo.toDto(track))
        }.describe {
            operationId = "getTrackById"
            summary = "Get a track by its Id"
            tag("Tracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<TrackDto>("The track")
                notFoundResponse("Track")
            }
        }

        // Get tracks by artist ID
        getP("/api/tracks/artists/{artistId}", PermissionType.VIEW_TRACKS) {
            val artistId = call.getLongParameter("artistId")
            val userId = call.getUserId() ?: unauthorized()

            val tracks = TrackRepo.getByArtistId(artistId, userId)
            call.respond(TrackRepo.toDto(tracks))
        }.describe {
            operationId = "getTrackByArtistId"
            summary = "Get all tracks from a specific artist"
            tag("Tracks")
            parameters {
                p("artistId", 1L)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<List<TrackDto>>("All tracks that artist is featured in")
                notFoundResponse("Track")
            }
        }

        // Edit track details
        putP("/api/tracks/{trackId}", PermissionType.MANAGE_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val editDto = call.receive<TrackEditDto>()
            editDto.validate()

            val updated = TrackRepo.update(trackId, editDto) ?: notFound()
            call.respond(TrackRepo.toDto(updated))
        }.describe {
            operationId = "updateTrack"
            summary = "Update a track"
            tag("Tracks")
            parameters {
                p("trackId", 1L)
            }
            requestBody {
                description = "The track edit data. Null values will not be updated."
                schema = jsonSchema<TrackEditDto>()
            }
            authResponses(PermissionType.MANAGE_TRACKS) {
                successResponse<TrackDto>("The updated track")
                notFoundResponse("Track")
            }
        }

        // Delete track
        deleteP("/api/tracks/{trackId}", PermissionType.DELETE_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()

            TrackRepo.delete(track)
            call.success()
        }.describe {
            operationId = "deleteTrack"
            summary = "Deletes a track"
            description = "This includes the file on disk and can not be undone."
            tag("Tracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.DELETE_TRACKS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Track")
            }
        }

        // Get related tracks
        getP("/api/tracks/{trackId}/similar", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val limit = call.getIntOrDefault("limit", 10)
            val userId = call.getUserId()

            val track = TrackRepo.getById(trackId) ?: notFound()
            val related = TrackRepo.getRelated(track, limit, userId)
            call.respond(TrackRepo.toMinimalDto(related))
        }.describe {
            operationId = "getSimilarTracks"
            summary = "Get similar tracks"
            tag("Tracks")
            parameters {
                p("trackId", 1L)
                q("limit", 10, required = false)
            }
            authResponses(PermissionType.DELETE_TRACKS) {
                successResponse<List<MinimalTrackDto>>("Similar tracks")
                notFoundResponse("Track")
            }
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
        }.describe {
            operationId = "searchTracks"
            summary = "Search tracks"
            tag("Tracks")
            parameters {
                paginatedSearch()
                q("sort", TrackSortingCriteria.TITLE, required = false)
                q("dir", SortDirection.ASC, required = false)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<PaginatedDto<MinimalTrackDto>>("A paginated list of tracks")
            }
        }

        getP("/api/tracks/random", PermissionType.VIEW_TRACKS) {
            val limit = call.getIntOrDefault("limit", 1)
            val userId = call.getUserId() ?: unauthorized()

            val track = TrackRepo.getRandom(limit, userId)

            call.respond(TrackRepo.toMinimalDto(track))
        }.describe {
            operationId = "getRandomTracks"
            summary = "Get random tracks"
            tag("Tracks")
            parameters {
                q("limit", 10, required = false,
                    description = "Maximum amount of random tracks to return")
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<List<MinimalTrackDto>>("Random tracks")
            }
        }

        getP("/api/tracks/newest", PermissionType.VIEW_TRACKS) {
            val limit = call.getIntOrDefault("limit", 10)
            val userId = call.getUserId() ?: unauthorized()

            val tracks = TrackRepo.getNewest(limit, userId)
            call.respond(TrackRepo.toMinimalDto(tracks))
        }.describe {
            operationId = "getNewestTrack"
            summary = "Get the most recently added tracks"
            tag("Tracks")
            parameters {
                q("limit", 10, required = false,
                    description = "Maximum amount of tracks to return")
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<List<MinimalTrackDto>>("Most recently added tracks")
            }
        }

        getP("/api/tracks/{trackId}/lyrics", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")

            val track = TrackRepo.getById(trackId) ?: notFound()
            val lyrics = LyricsRepo.getLyrics(track)?.content
            val colorScheme = TrackRepo.getColorScheme(track)

            call.respond(LyricsDto(
                lyrics = lyrics,
                colorScheme = colorScheme?.let { ColorSchemeRepo.toDto(it) }
            ))
        }.describe {
            operationId = "getLyrics"
            summary = "Get a track's lyrics"
            tag("Tracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<LyricsDto>("Lyrics and colorscheme")
                notFoundResponse("Track")
            }
        }

        getP("/api/tracks/{trackId}/lyrics/check", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val hasLyrics = LyricsRepo.hasLyrics(trackId)
            call.success(hasLyrics)
        }.describe {
            operationId = "checkLyrics"
            summary = "Check if track has lyrics"
            tag("Tracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<SuccessDto>("True if the track has lyrics, false otherwise")
            }
        }

        getP("/api/tracks/{trackId}/download", PermissionType.DOWNLOAD_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()

            val audioFile = PathUtils.getTrackFileFromPath(track.path)
            if (!audioFile.exists()) {
                notFound()
            }

            call.respondFile(audioFile)
        }.describe {
            operationId = "downloadTrack"
            summary = "Download a track"
            tag("Tracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.DOWNLOAD_TRACKS) {
                successResponse<ByteArray>("Track data")
                notFoundResponse("Track")
            }
        }

        getP("/api/tracks/{trackId}/cover", PermissionType.VIEW_TRACKS) {
            val trackId = call.getLongParameter("trackId")
            val quality = call.getIntOrDefault("quality", 0)
            val imageId = TrackRepo.getCoverId(trackId)

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=track&quality=$quality")
        }.describe {
            operationId = "getTrackCover"
            summary = "Get a track cover"
            description = "Redirects to the cover image of a track, or the default placeholder if the track does not have a cover"
            tag("Tracks")
            parameters {
                p("albumId", 1L)
                q("quality", 0, required = false,
                    description = "The width and height of the image. 0 means the original size.")
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                redirectResponse("`/api/images/by-id/{imageId}` or `/api/images/default`")
            }
        }

        postP("/api/tracks/edit/batch", PermissionType.MANAGE_TRACKS) {
            val userId = call.getUserId()
                ?: unauthorized()

            val editDto = call.receive<BatchEditDto>()

            val result = BatchEditRepo.batchEdit(editDto, userId)

            call.respond(result)
        }.describe {
            operationId = "batchEditTracks"
            summary = "Batch-edit tracks"
            tag("Tracks")
            requestBody {
                description = "A list of actions to perform on multiple tracks"
                schema = jsonSchema<BatchEditDto>()
            }
            authResponses(PermissionType.MANAGE_TRACKS) {
                successResponse<BatchEditResultDto>("Result")
            }
        }
    }
}