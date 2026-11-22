package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.TrackRelationRepo
import de.mickkc.vibin.repos.TrackRepo
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRelatedTrackRoutes() = routing {

    authenticate("tokenAuth") {

        getP("/api/tracks/{trackId}/related", PermissionType.VIEW_TRACKS) {

            val trackId = call.getLongParameter("trackId")
            val relatedTracks = TrackRelationRepo.getRelatedTracks(trackId)

            call.respond(relatedTracks.map {
                KeyValueDto(key = TrackRepo.toMinimalDto(it.first), value = it.second)
            })
        }.describe {
            operationId = "getRelatedTracks"
            summary = "Get related tracks"
            tag("RelatedTracks")
            parameters {
                p("trackId", 1L)
            }
            authResponses(PermissionType.VIEW_TRACKS) {
                successResponse<List<KeyValueDto<MinimalTrackDto, String>>>("A list of key-value pairs, where the key is a minimal " +
                        "track and the value is the the relation description (e.g. \"Remix\")")
            }
        }

        postP("/api/tracks/{trackId}/related", PermissionType.CREATE_TRACK_RELATIONS) {

            val trackId = call.getLongParameter("trackId")
            val relatedTrackId = call.getLongParameter("relatedTrackId")
            val relationDescription = call.getStringParameter("description")
            val mutual = call.getBooleanOrDefault("mutual", false)
            val reverseDescription = call.parameters["reverseDescription"]

            if (trackId == relatedTrackId) {
                invalidParameter("relatedTrackId", "relatedTrackId != trackId")
            }

            if (TrackRelationRepo.doesRelationExist(trackId, relatedTrackId)) {
                conflict("The two tracks are already marked as related.")
            }

            val track = TrackRepo.getById(trackId) ?: notFound()
            val relatedTrack = TrackRepo.getById(relatedTrackId) ?: notFound()

            TrackRelationRepo.createRelation(
                track = track,
                relatedTrack = relatedTrack,
                description = relationDescription,
                mutual = mutual,
                reverseDescription = reverseDescription
            )

            call.success()
        }.describe {
            operationId = "createTrackRelation"
            summary = "Create a track relation"
            tag("RelatedTracks")
            parameters {
                p("trackId", 1L)
                q("relatedTrackId", 2L, required = true)
                q("description", "Remix", required = true,
                    description = "The relation description from the track with `trackId` to the track with `relatedTrackId`.")
                q("mutual", true, required = true,
                    description = "Whether the other track should also be marked as related to this track")
                q("reverseDescription", "Original", required = false,
                    description = "The relation description from the track with `relatedTrackId` to the track with `trackId`. " +
                            "If mutual is false, this does not have any effect. If mutual is true and this parameter is " +
                            "not provided, the description will be used instead.")
            }
            authResponses(PermissionType.CREATE_TRACK_RELATIONS) {
                successResponse<SuccessDto>("Success")
                invalidParameterResponse("relatedTrackId", "trackId and relatedTrackId can not point to the same track.")
                notFoundResponse("Track or related track")
            }
        }

        deleteP("/api/tracks/{trackId}/related", PermissionType.DELETE_TRACK_RELATIONS) {

            val trackId = call.getLongParameter("trackId")
            val relatedTrackId = call.getLongParameter("relatedTrackId")

            TrackRelationRepo.removeRelationBetweenTracks(trackId, relatedTrackId)

            call.success()
        }.describe {
            operationId = "removeTrackRelation"
            summary = "Remove a track relation"
            tag("RelatedTracks")
            parameters {
                p("trackId", 1L)
                q("relatedTrackId", 1L, required = true)
            }
            authResponses(PermissionType.DELETE_TRACK_RELATIONS) {
                successResponse<SuccessDto>("Success")
            }
        }
    }
}