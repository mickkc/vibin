package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRelationRepo
import de.mickkc.vibin.repos.TrackRepo
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

fun Application.configureRelatedTrackRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/tracks/{trackId}/related", PermissionType.VIEW_TRACKS) {

            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@getP call.missingParameter("trackId")

            val relatedTracks = TrackRelationRepo.getRelatedTracks(trackId)

            call.respond(relatedTracks.map {
                KeyValueDto(key = TrackRepo.toMinimalDto(it.first), value = it.second)
            })
        }

        postP("/api/tracks/{trackId}/related", PermissionType.CREATE_TRACK_RELATIONS) {

            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@postP call.missingParameter("trackId")

            val relatedTrackId = call.parameters["relatedTrackId"]?.toLongOrNull() ?: return@postP call.missingParameter("relatedTrackId")
            val relationDescription = call.parameters["description"] ?: return@postP call.missingParameter("description")
            val mutual = call.parameters["mutual"]?.toBoolean() ?: false
            val reverseDescription = call.parameters["reverseDescription"]

            if (trackId == relatedTrackId) {
                return@postP call.invalidParameter("relatedTrackId", "relatedTrackId != trackId")
            }

            if (TrackRelationRepo.doesRelationExist(trackId, relatedTrackId)) {
                return@postP call.conflict()
            }

            val track = TrackRepo.getById(trackId) ?: return@postP call.notFound()
            val relatedTrack = TrackRepo.getById(relatedTrackId) ?: return@postP call.notFound()

            TrackRelationRepo.createRelation(
                track = track,
                relatedTrack = relatedTrack,
                description = relationDescription,
                mutual = mutual,
                reverseDescription = reverseDescription
            )

            call.success()
        }

        deleteP("/api/tracks/{trackId}/related", PermissionType.DELETE_TRACK_RELATIONS) {

            val trackId = call.parameters["trackId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("trackId")
            val relatedTrackId = call.parameters["relatedTrackId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("relatedTrackId")

            TrackRelationRepo.removeRelationBetweenTracks(trackId, relatedTrackId)

            call.success()
        }
    }
}