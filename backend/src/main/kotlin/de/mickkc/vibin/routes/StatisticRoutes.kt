package de.mickkc.vibin.routes

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.*
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.*
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.user.ShowActivitiesToOthers
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*

@OptIn(ExperimentalKtorApi::class)
fun Application.configureStatisticRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/stats/recent") {
            val userId = call.getUserId() ?: unauthorized()
            val limit = call.getIntOrDefault("limit", 5)

            val recentTracks = ListenRepo.getRecentTracks(userId, 0, limit, userId)
            call.respond(TrackRepo.toMinimalDto(recentTracks))
        }.describe {
            operationId = "getRecentTracks"
            summary = "Get recently listened to tracks"
            tag("Statistics")
            parameters {
                q("limit", 5, required = false)
            }
            authResponses {
                successResponse<List<MinimalTrackDto>>("List of recently listened to tracks")
            }
        }

        get("/api/stats/recent/nontracks") {
            val userId = call.getUserId() ?: unauthorized()
            val limit = call.getIntOrDefault("limit", 5)

            val recent = ListenRepo.getRecentNonTrackDtos(userId, limit, userId)
            call.respond(recent)
        }.describe {
            operationId = "getRecentNonTracks"
            summary = "Get recently listened to albums, artists and playlists"
            tag("Statistics")
            parameters {
                q("limit", 5, required = false)
            }
            authResponses {
                successResponse<List<KeyValueDto<String, AlbumDto>>>(
                    "A list of key-value pairs, where the key is the type (`ALBUM`, `ARTIST`, `PLAYLIST`) " +
                            "and the value is the actual album/artist/playlist object."
                )
            }
        }

        get("/api/stats/{type}/top{num}") {
            val type = call.getStringParameter("type")
            val num = call.getIntParameter("num")
            val since = call.getLongOrDefault("since", DateTimeUtils.startOfMonth())

            if (num <= 0) {
                invalidParameter("num", "num > 0")
            }

            val userId = call.getUserId() ?: unauthorized()


            fun <T>sortAndLimit(map: Map<T, Int>): List<T> {
                val sorted = map.toList().sortedByDescending { (_, count) -> count }.map { it.first }
                return sorted.take(num)
            }

            val response = when (type) {
                "tracks" -> {
                    val topTracks = ListenRepo.getMostListenedTracks(userId, since, userId)
                    TrackRepo.toMinimalDto(sortAndLimit(topTracks))
                }
                "artists" -> {
                    val topArtists = ListenRepo.getMostListenedArtistsByTracks(userId, since, userId)
                    ArtistRepo.toDto(sortAndLimit(topArtists))
                }
                "albums" -> {
                    val topAlbums = ListenRepo.getMostListenedAlbums(userId, since, userId)
                    AlbumRepo.toDto(sortAndLimit(topAlbums))
                }
                "tags" -> {
                    val topTags = ListenRepo.getMostListenedTags(userId, since, userId)
                    TagRepo.toDto(sortAndLimit(topTags))
                }
                "playlists" -> {
                    val topPlaylists = ListenRepo.getMostListenedPlaylists(userId, since)
                    PlaylistRepo.toDto(sortAndLimit(topPlaylists))
                }
                "nontracks" -> {
                    val top = ListenRepo.getMostListenedToAsDtos(userId, since, userId)
                    sortAndLimit(top)
                }
                "global_nontracks" -> {
                    val top = ListenRepo.getMostListenedToAsDtos(userId, since, global = true, requestingUserId = userId)
                    sortAndLimit(top)
                }
                else -> {
                    invalidParameter("type", "nontracks")
                }
            }

            call.respond(response)
        }.describe {
            operationId = "getTopStats"
            summary = "Get the top N objects of a specific type"
            tag("Statistics")
            parameters {
                p("type", "tracks", description = "One of the following types:\n" +
                        "- `tracks`\n" +
                        "- `artists`\n" +
                        "- `albums`\n" +
                        "- `tags`\n" +
                        "- `playlists`\n" +
                        "- `nontracks`: artists, albums and playlists\n" +
                        "- `global_nontracks`: artists, albums and playlists across all users")
                p("num", 10, "Maximum amount of returned items")
                q("since", 1782511260L, required = false)
            }
            responses {
                successResponse<List<MinimalTrackDto>>("A list of items of `type`, sorted by most listened")
                invalidParameterResponse("num", "num is not a positive integer")
            }
        }

        post("/api/stats/listen/{type}/{entityId}") {
            val userId = call.getUserId() ?: unauthorized()
            val type = call.getEnum<ListenType>("type", null)
            val entityId = call.getLongParameter("entityId")

            val success = ListenRepo.listenedTo(userId, entityId, type)
            call.success(success)
        }.describe {
            operationId = "recordListen"
            summary = "Record a listen to a specific entity"
            tag("Statistics")
            parameters {
                p("type", ListenType.TRACK)
                p("entityType", 1L)
            }
            responses {
                successResponse<SuccessDto>("True if the listen was recorded, false if not " +
                        "(listening to the same track in a short timespan does not count)")
            }
        }

        get("/api/stats/users/{userId}/activity") {
            val userId = call.getLongParameter("userId")

            if (!Settings.get(ShowActivitiesToOthers, userId)) {
                val callerId = call.getUserId() ?: unauthorized()
                if (callerId != userId) {
                    forbidden("User has disabled showing activities to others")
                }
            }

            val since = call.getLongOrDefault("since", DateTimeUtils.startOfMonth())
            val limit = call.getIntOrDefault("limit", 10)

            if (limit <= 0) {
                invalidParameter("limit", "limit > 0")
            }

            val activity = ListenRepo.getActivityForUser(userId, since, limit, userId)

            call.respond(UserActivityDto(
                recentTracks = TrackRepo.toMinimalDto(activity.recentTracks),
                topTracks = TrackRepo.toMinimalDto(activity.topTracks),
                topArtists = ArtistRepo.toDto(activity.topArtists)
            ))
        }.describe {
            operationId = "getUserActivity"
            summary = "Get a user's listening activity"
            tag("Statistics")
            parameters {
                p("userId", 1L)
                q("since", 1782511260L, required = false)
                q("limit", 10, required = false)
            }
            responses {
                successResponse<UserActivityDto>("The user's listening activity")
                forbiddenResponse("User has disabled showing activities to others")
                invalidParameterResponse("limit", "limit is not a positive integer")
            }
        }

        get("/api/stats/global") {
            return@get call.respond(StatisticsDto(
                totalTracks = TrackRepo.count(),
                totalTrackDuration = TrackRepo.getTotalRuntimeSeconds(),
                totalAlbums = AlbumRepo.count(),
                totalArtists = ArtistRepo.count(),
                totalPlaylists = PlaylistRepo.count(),
                totalUsers = UserRepo.count(),
                totalPlays = ListenRepo.getTotalListenedTracks(),
                totalListenDuration = ListenTrackingRepo.getGlobalListenDuration()
            ))
        }.describe {
            operationId = "getGlobalStatistics"
            summary = "Get the global server statistics"
            tag("Statistics")
            responses {
                successResponse<StatisticsDto>("The global server statistics")
            }
        }

        getP("/api/stats/tracks/{id}/duration", PermissionType.VIEW_TRACKS) {
            val userId = call.getUserId() ?: unauthorized()
            val trackId = call.pathParameters["id"]?.toLongOrNull() ?: missingParameter("id")

            val duration = ListenTrackingRepo.getUserListenDurationForTrack(trackId, userId)

            call.respond(ListenDurationDto(
                duration = duration
            ))
        }.describe {
            operationId = "getTrackListeningDuration"
            summary = "Get a users total listening duration for a track"
            tag("Statistics")
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<ListenDurationDto>("Total listening duration in seconds")
            }
        }
    }
}