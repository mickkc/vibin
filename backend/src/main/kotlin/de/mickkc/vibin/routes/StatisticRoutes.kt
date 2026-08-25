package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.StatisticsDto
import de.mickkc.vibin.dto.UserActivityDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.repos.ListenTrackingRepo
import de.mickkc.vibin.repos.PlaylistRepo
import de.mickkc.vibin.repos.TagRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.user.ShowActivitiesToOthers
import de.mickkc.vibin.utils.DateTimeUtils

fun Application.configureStatisticRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/stats/recent") {
            val userId = call.getUserId() ?: unauthorized()
            val limit = call.getIntOrDefault("limit", 5)

            val recentTracks = ListenRepo.getRecentTracks(userId, 0, limit)
            call.respond(TrackRepo.toMinimalDto(recentTracks))
        }

        get("/api/stats/recent/nontracks") {
            val userId = call.getUserId() ?: unauthorized()
            val limit = call.getIntOrDefault("limit", 5)

            val recent = ListenRepo.getRecentNonTrackDtos(userId, limit)
            call.respond(recent)
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
                    val topTracks = ListenRepo.getMostListenedTracks(userId, since)
                    TrackRepo.toMinimalDto(sortAndLimit(topTracks))
                }
                "artists" -> {
                    val topArtists = ListenRepo.getMostListenedArtistsByTracks(userId, since)
                    ArtistRepo.toDto(sortAndLimit(topArtists))
                }
                "albums" -> {
                    val topAlbums = ListenRepo.getMostListenedAlbums(userId, since)
                    AlbumRepo.toDto(sortAndLimit(topAlbums))
                }
                "tags" -> {
                    val topTags = ListenRepo.getMostListenedTags(userId, since)
                    TagRepo.toDto(sortAndLimit(topTags))
                }
                "playlists" -> {
                    val topPlaylists = ListenRepo.getMostListenedPlaylists(userId, since)
                    PlaylistRepo.toDto(sortAndLimit(topPlaylists))
                }
                "nontracks" -> {
                    val top = ListenRepo.getMostListenedToAsDtos(userId, since)
                    sortAndLimit(top)
                }
                "global_nontracks" -> {
                    val top = ListenRepo.getMostListenedToAsDtos(userId, since, global = true)
                    sortAndLimit(top)
                }
                else -> {
                    invalidParameter("type", "nontracks")
                }
            }

            call.respond(response)
        }

        post("/api/stats/listen/{type}/{entityId}") {
            val userId = call.getUserId() ?: unauthorized()
            val type = call.parameters["type"]?.let {
                try { ListenType.valueOf(it) } catch (_: IllegalArgumentException) { null }
            } ?: missingParameter("type")
            val entityId = call.getLongParameter("entityId")

            val success = ListenRepo.listenedTo(userId, entityId, type)
            call.success(success)
        }

        get("/api/stats/users/{userId}/activity") {
            val userId = call.getLongParameter("userId")

            if (!Settings.get(ShowActivitiesToOthers, userId)) {
                val callerId = call.getUserId() ?: unauthorized()
                if (callerId != userId) {
                    // User has disabled showing activities to others
                    forbidden()
                }
            }

            val since = call.getLongOrDefault("since", DateTimeUtils.startOfMonth())
            val limit = call.getIntOrDefault("limit", 10)

            if (limit <= 0) {
                invalidParameter("limit", "limit > 0")
            }

            val activity = ListenRepo.getActivityForUser(userId, since, limit)

            call.respond(UserActivityDto(
                recentTracks = TrackRepo.toMinimalDto(activity.recentTracks),
                topTracks = TrackRepo.toMinimalDto(activity.topTracks),
                topArtists = ArtistRepo.toDto(activity.topArtists)
            ))
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
        }

        getP("/api/stats/tracks/{id}/duration", PermissionType.VIEW_TRACKS) {
            val userId = call.getUserId() ?: forbidden()
            val trackId = call.pathParameters["id"]?.toLongOrNull() ?: missingParameter("id")

            val duration = ListenTrackingRepo.getUserListenDurationForTrack(trackId, userId)

            call.respond(mapOf(
                "duration" to duration
            ))
        }
    }
}