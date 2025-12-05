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

            val trackId = call.getLongParameter("trackId") ?: return@getP
            val relatedTracks = TrackRelationRepo.getRelatedTracks(trackId)

            call.respond(relatedTracks.map {
                KeyValueDto(key = TrackRepo.toMinimalDto(it.first), value = it.second)
            })
        }

        postP("/api/tracks/{trackId}/related", PermissionType.CREATE_TRACK_RELATIONS) {

            val trackId = call.getLongParameter("trackId") ?: return@postP
            val relatedTrackId = call.getLongParameter("relatedTrackId") ?: return@postP
            val relationDescription = call.getStringParameter("description") ?: return@postP
            val mutual = call.getBooleanOrDefault("mutual", false) ?: return@postP
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

            val trackId = call.getLongParameter("trackId") ?: return@deleteP
            val relatedTrackId = call.getLongParameter("relatedTrackId") ?: return@deleteP

            TrackRelationRepo.removeRelationBetweenTracks(trackId, relatedTrackId)

            call.success()
        }
    }
}