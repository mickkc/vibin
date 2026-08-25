package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import de.mickkc.vibin.db.playlists.PlaylistEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PlaylistRepo
import de.mickkc.vibin.repos.PlaylistTrackRepo
import de.mickkc.vibin.repos.TrackRepo

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
        }

        deleteP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val (playlist, track) = call.getPlaylistAndTrack()

            PlaylistTrackRepo.removeTrackFromPlaylist(playlist, track)
            call.success()
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
        }

        getP("/api/playlists/containing/{trackId}", PermissionType.VIEW_PLAYLISTS) {
            val trackId = call.getLongParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: notFound()
            val userId = call.getUserId() ?: unauthorized()

            val playlists = PlaylistTrackRepo.getPlaylistsWithTrack(track, userId)
            call.respond(PlaylistRepo.toDto(playlists))
        }
    }
}