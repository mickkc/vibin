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

    suspend fun RoutingCall.getPlaylistAndTrack(): Pair<PlaylistEntity, TrackEntity>? {

        val userId = getUserId() ?: return null

        val playlistId = parameters["playlistId"]?.toLongOrNull()
            ?: return missingParameter("playlistId").let { null }
        val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, userId, PermissionType.MANAGE_PLAYLIST_TRACKS)
            ?: return notFound().let { null }

        val trackId = request.queryParameters["trackId"]?.toLongOrNull()
            ?: return missingParameter("trackId").let { null }
        val track = TrackRepo.getById(trackId) ?: return notFound().let { null }

        return playlist to track
    }

    authenticate("tokenAuth") {

        postP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val user = call.getUser() ?: return@postP call.unauthorized()
            val (playlist, track) = call.getPlaylistAndTrack() ?: return@postP

            PlaylistTrackRepo.addTrackToPlaylist(playlist, track, user)
            call.success()
        }

        deleteP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val (playlist, track) = call.getPlaylistAndTrack() ?: return@deleteP

            PlaylistTrackRepo.removeTrackFromPlaylist(playlist, track)
            call.success()
        }

        putP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLIST_TRACKS) {

            val afterTrackId: Long? = call.request.queryParameters["afterTrackId"]?.toLongOrNull()

            val (playlist, track) = call.getPlaylistAndTrack() ?: return@putP
            val afterTrack = afterTrackId?.let { TrackRepo.getById(afterTrackId) ?: return@putP call.notFound() }

            val success = PlaylistTrackRepo.setPosition(playlist, track, afterTrack)
            if (!success) {
                return@putP call.invalidParameter("newPosition")
            }

            val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
            call.respond(playlistTracks)
        }

        getP("/api/playlists/containing/{trackId}", PermissionType.VIEW_PLAYLISTS) {
            val trackId = call.getLongParameter("trackId") ?: return@getP
            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()
            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val playlists = PlaylistTrackRepo.getPlaylistsWithTrack(track, userId)
            call.respond(PlaylistRepo.toDto(playlists))
        }
    }
}