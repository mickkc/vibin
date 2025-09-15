package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.repos.ArtistRepo
import wtf.ndu.vibin.repos.ListenRepo
import wtf.ndu.vibin.repos.TagRepo
import wtf.ndu.vibin.repos.TrackRepo
import wtf.ndu.vibin.utils.DateTimeUtils

fun Application.configureStatisticRoutes() = routing {

    authenticate("tokenAuth") {

        get("/api/stats/recent") {
            val userId = call.getUserId() ?: return@get call.unauthorized()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 5

            val recentTracks = ListenRepo.getRecentTracks(userId, limit)
            call.respond(TrackRepo.toMinimalDto(recentTracks))
        }

        get("/api/stats/{type}/top{num}") {
            val type = call.parameters["type"] ?: return@get call.missingParameter("type")
            val num = call.parameters["num"]?.toIntOrNull() ?: return@get call.missingParameter("num")
            val since = call.request.queryParameters["since"]?.toLongOrNull() ?: DateTimeUtils.startOfMonth()

            if (num <= 0) {
                return@get call.invalidParameter("num", "num > 0")
            }

            val userId = call.getUserId() ?: return@get call.unauthorized()

            val topTracks = ListenRepo.getMostListenedTracks(userId, since)

            fun <T>sortAndLimit(map: Map<T, Int>): List<T> {
                val sorted = map.toList().sortedByDescending { (_, count) -> count }.map { it.first }
                return sorted.take(num)
            }

            val response = when (type) {
                "tracks" -> {
                    TrackRepo.toMinimalDto(sortAndLimit(topTracks))
                }
                "artists" -> {
                    val topArtists = ListenRepo.getMostListenedArtists(topTracks)
                    ArtistRepo.toDto(sortAndLimit(topArtists))
                }
                "albums" -> {
                    val topAlbums = ListenRepo.getMostListenedAlbums(topTracks)
                    AlbumRepo.toDto(sortAndLimit(topAlbums))
                }
                "tags" -> {
                    val topTags = ListenRepo.getMostListenedTags(topTracks)
                    TagRepo.toDto(sortAndLimit(topTags))
                }
                else -> {
                    call.invalidParameter("type", "tracks", "artists", "albums", "tags")
                    null
                }
            }

            if (response != null) {
                call.respond(response)
            }
        }

    }

}