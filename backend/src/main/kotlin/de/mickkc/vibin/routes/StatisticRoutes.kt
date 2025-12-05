package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.StatisticsDto
import de.mickkc.vibin.dto.UserActivityDto
import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.ListenRepo
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
            val userId = call.getUserId() ?: return@get call.unauthorized()
            val limit = call.getIntOrDefault("limit", 5) ?: return@get

            val recentTracks = ListenRepo.getRecentTracks(userId, 0, limit)
            call.respond(TrackRepo.toMinimalDto(recentTracks))
        }

        get("/api/stats/recent/nontracks") {
            val userId = call.getUserId() ?: return@get call.unauthorized()
            val limit = call.getIntOrDefault("limit", 5) ?: return@get

            val recent = ListenRepo.getRecentNonTrackDtos(userId, limit)
            call.respond(recent)
        }

        get("/api/stats/{type}/top{num}") {
            val type = call.getStringParameter("type") ?: return@get
            val num = call.getIntParameter("num") ?: return@get
            val since = call.getLongOrDefault("since", DateTimeUtils.startOfMonth()) ?: return@get

            if (num <= 0) {
                return@get call.invalidParameter("num", "num > 0")
            }

            val userId = call.getUserId() ?: return@get call.unauthorized()


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
                    call.invalidParameter("type", "tracks", "artists", "albums", "tags", "nontracks")
                    null
                }
            }

            if (response != null) {
                call.respond(response)
            }
        }

        post("/api/stats/listen/{type}/{entityId}") {
            val userId = call.getUserId() ?: return@post call.unauthorized()
            val type = call.parameters["type"]?.let {
                try { ListenType.valueOf(it) } catch (_: IllegalArgumentException) { null }
            } ?: return@post call.missingParameter("type")
            val entityId = call.getLongParameter("entityId") ?: return@post

            val success = ListenRepo.listenedTo(userId, entityId, type)
            call.success(success)
        }

        get("/api/stats/users/{userId}/activity") {
            val userId = call.getLongParameter("userId") ?: return@get

            if (!Settings.get(ShowActivitiesToOthers, userId)) {
                val callerId = call.getUserId() ?: return@get call.unauthorized()
                if (callerId != userId) {
                    // User has diasabled showing activities to others
                    return@get call.forbidden()
                }
            }

            val since = call.getLongOrDefault("since", DateTimeUtils.startOfMonth()) ?: return@get
            val limit = call.getIntOrDefault("limit", 10) ?: return@get

            if (limit <= 0) {
                return@get call.invalidParameter("limit", "limit > 0")
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
                totalPlays = ListenRepo.getTotalListenedTracks()
            ))
        }
    }
}