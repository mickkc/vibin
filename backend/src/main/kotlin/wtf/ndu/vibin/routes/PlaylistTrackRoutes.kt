package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PlaylistRepo
import wtf.ndu.vibin.repos.PlaylistTrackRepo
import wtf.ndu.vibin.repos.TrackRepo

fun Application.configurePlaylistTrackRoutes() = routing {

    suspend fun RoutingCall.getPlaylistAndTrack(): Pair<PlaylistEntity, TrackEntity>? {

        val userId = getUserId() ?: return null

        val playlistId = parameters["playlistId"]?.toLongOrNull()
            ?: return missingParameter("playlistId").let { null }
        val playlist = PlaylistRepo.getByIdIfAllowed(playlistId, userId, PermissionType.EDIT_COLLABORATIVE_PLAYLISTS)
            ?: return notFound().let { null }

        val trackId = request.queryParameters["trackId"]?.toLongOrNull()
            ?: return missingParameter("trackId").let { null }
        val track = TrackRepo.getById(trackId) ?: return notFound().let { null }

        return playlist to track
    }

    authenticate("tokenAuth") {

        postP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLISTS) {

            val (playlist, track) = call.getPlaylistAndTrack() ?: return@postP

            PlaylistTrackRepo.addTrackToPlaylist(playlist, track)
            call.success()
        }

        deleteP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLISTS) {

            val (playlist, track) = call.getPlaylistAndTrack() ?: return@deleteP

            PlaylistTrackRepo.removeTrackFromPlaylist(playlist, track)
            call.success()
        }

        putP("/api/playlists/{playlistId}/tracks", PermissionType.MANAGE_PLAYLISTS) {

            val newPosition = call.request.queryParameters["newPosition"]?.toIntOrNull()
                ?: return@putP call.missingParameter("newPosition")
            val (playlist, track) = call.getPlaylistAndTrack() ?: return@putP

            val success = PlaylistTrackRepo.setPosition(playlist, track, newPosition)
            call.success(success)
        }

        getP("/api/playlists/containing/{trackId}", PermissionType.VIEW_PLAYLISTS) {
            val trackId = call.parameters["trackId"]?.toLongOrNull()
                ?: return@getP call.missingParameter("trackId")
            val track = TrackRepo.getById(trackId) ?: return@getP call.notFound()

            val playlists = PlaylistTrackRepo.getPlaylistsWithTrack(track)
            call.respond(PlaylistRepo.toDto(playlists))
        }
    }
}