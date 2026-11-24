package de.mickkc.vibin.routes

import de.mickkc.vibin.db.playlists.PlaylistEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.playlists.PlaylistDto
import de.mickkc.vibin.dto.playlists.PlaylistTrackDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PlaylistRepo
import de.mickkc.vibin.repos.PlaylistTrackRepo
import de.mickkc.vibin.repos.TrackRepo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.*

@OptIn(ExperimentalKtorApi::class)
fun Application.configurePlaylistTrackRoutes() = routing {

    fun RoutingCall.getPlaylistAndTrack(): Pair<PlaylistEntity, TrackEntity> {

        val userId = getUserId() ?: unauthorized()

        val playlistId = parameters["playlistId"]?.toLongOrNull()
            ?: missingParameter("playlistId")
        val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, userId, PermissionType.MANAGE_PLAYLIST_TRACKS)
            ?: notFound()

        val trackId = request.queryParameters["trackId"]?.toLongOrNull()
            ?: missingParameter("trackId")
        val track = TrackRepo.getById(trackId) ?: notFound()

        return playlist to track
    }

    authenticate("tokenAuth") {

        postP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val user = call.getUser() ?: unauthorized()
            val (playlist, track) = call.getPlaylistAndTrack()

            PlaylistTrackRepo.addTrackToPlaylist(playlist, track, user)
            call.success()
        }.describe {
            operationId = "addTrackToPlaylist"
            summary = "Add a track to a playlist"
            tag("PlaylistTracks")
            parameters {
                p("playlistId", 1L)
                q("trackId", 1L, required = true)
            }
            authResponses(PermissionType.MANAGE_PLAYLIST_TRACKS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Playlist or track")
            }
        }

        deleteP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val (playlist, track) = call.getPlaylistAndTrack()

            PlaylistTrackRepo.removeTrackFromPlaylist(playlist, track)
            call.success()
        }.describe {
            operationId = "removeTrackFromPlaylist"
            summary = "Remove a track from a playlist"
            tag("PlaylistTracks")
            parameters {
                p("playlistId", 1L)
                q("trackId", 1L, required = true)
            }
            authResponses(PermissionType.MANAGE_PLAYLIST_TRACKS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Playlist or track")
            }
        }

        putP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val afterTrackId: Long? = call.request.queryParameters["afterTrackId"]?.toLongOrNull()

            val (playlist, track) = call.getPlaylistAndTrack()
            val afterTrack = afterTrackId?.let { TrackRepo.getById(afterTrackId) ?: notFound() }

            val success = PlaylistTrackRepo.setPosition(playlist, track, afterTrack)
            if (!success) {
                invalidParameter("newPosition")
            }

            val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
            call.respond(playlistTracks)
        }.describe {
            operationId = "moveTrackInPlaylist"
            summary = "Move a track inside a playlist"
            tag("PlaylistTracks")
            parameters {
                p("playlistId", 1L)
                q("trackId", 1L, required = true)
                q("afterTrackId", 2L, required = false,
                    description = "The Id of the track before the moved track. If null, the track will be moved to the start.")
            }
            authResponses(PermissionType.MANAGE_PLAYLIST_TRACKS) {
                successResponse<List<PlaylistTrackDto>>("The tracks in their new order")
                notFoundResponse("Playlist or track")
            }
        }

        getP("/api/playlists/containing/{trackId}", PermissionType.VIEW_PLAYLISTS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()
            val userId = call.getUserId() ?: unauthorized()

            val playlists = PlaylistTrackRepo.getPlaylistsWithTrack(track, userId)
            call.respond(PlaylistRepo.toDto(playlists))
        }.describe {
            operationId = "getPlaylistsContainingTrack"
            summary = "Get playlists containing a track"
            tag("PlaylistTracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.VIEW_PLAYLISTS) {
                successResponse<List<PlaylistDto>>("A list of all playlists containing the track")
                notFoundResponse("Track")
            }
        }
    }
}