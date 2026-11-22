package de.mickkc.vibin.routes

import de.mickkc.vibin.IsFavoriteDto
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.routing
import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.dto.FavoriteDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.FavoriteRepo
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureFavoriteRoutes() = routing {
    authenticate("tokenAuth") {

        fun RoutingCall.getFavoriteType(): FavoriteType = getEnum<FavoriteType>("entityType", null)

        getP("/api/favorites/{userId}") {
            val requestingUserId = call.getUserId()
            val userId = call.getLongParameter("userId")

            if (requestingUserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                noPermission(PermissionType.VIEW_USERS)
            }

            val favorites = FavoriteRepo.getFavoriteDtoForUser(userId, requestingUserId)

            call.respond(favorites)
        }.describe {
            operationId = "getFavorites"
            summary = "Get favorites"
            tag("Favorites")
            parameters {
                p("userId", 1L)
            }
            authResponses(PermissionType.VIEW_USERS) {
                successResponse<FavoriteDto>("Favorites")
            }
            optionalPermissions(
                PermissionType.VIEW_USERS to "View the favorites of users other than yourself."
            )
        }

        putP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: unauthorized()
            val entityId = call.getLongParameter("entityId")
            val place = call.getIntParameter("place")

            if (place !in 1..3)
                invalidParameter("place", "1", "2", "3")

            val entityType = call.getFavoriteType()

            FavoriteRepo.addFavorite(userId, entityType, entityId, place)

            call.success()
        }.describe {
            operationId = "setFavorite"
            summary = "Set a favorite track, artist or album"
            description = "The track, artist or album previously on that place will be overridden."
            tag("Favorites")
            parameters {
                p("entityType", FavoriteType.TRACK)
                p("place", 1, description = "The place from 1 to 3.")
                q("entityId", 1L, required = true, description = "The Id of the track, artist or album")
            }
            authResponses {
                successResponse<SuccessDto>("Success")
                invalidParameterResponse("place", "Place is below 1 or above 3.")
            }
        }

        deleteP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: unauthorized()
            val place = call.getIntParameter("place")

            if (place !in 1..3)
                invalidParameter("place", "3")

            val entityType = call.getFavoriteType()

            FavoriteRepo.deleteFavoriteAtPlace(userId, entityType, place)

            call.success()
        }.describe {
            operationId = "removeFavorite"
            summary = "Remove a track, artist or album from favorites"
            tag("Favorites")
            parameters {
                p("entityType", FavoriteType.TRACK)
                p("place", 1, description = "The place from 1 to 3.")
            }
            authResponses {
                successResponse<SuccessDto>("Success")
                invalidParameterResponse("place", "Place is below 1 or above 3.")
            }
        }

        getP("/api/favorites/{entityType}/check/{entityId}") {

            val userId = call.getUserId() ?: unauthorized()

            val entityId = call.getLongParameter("entityId")

            val entityType = call.getFavoriteType()

            val place = FavoriteRepo.getPlace(userId, entityType, entityId)

            call.respond(IsFavoriteDto(
                isFavorite = place != null,
                place = place
            ))
        }.describe {
            operationId = "checkFavorite"
            summary = "Check if a track, artist or album is marked as favorite"
            tag("Favorites")
            parameters {
                p("entityType", FavoriteType.TRACK)
                p("entityId", 1L, description = "The id of the track, artist or album")
            }
            authResponses {
                successResponse<IsFavoriteDto>("Favorite status")
            }
        }
    }
}