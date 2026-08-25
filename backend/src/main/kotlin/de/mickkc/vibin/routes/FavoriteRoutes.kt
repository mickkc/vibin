package de.mickkc.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.routing
import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.FavoriteRepo

fun Application.configureFavoriteRoutes() = routing {
    authenticate("tokenAuth") {

        fun RoutingCall.getFavoriteType(): FavoriteType {
            val entityTypeParam = getStringParameter("entityType")

            return when (entityTypeParam.lowercase()) {
                "track" -> FavoriteType.TRACK
                "album" -> FavoriteType.ALBUM
                "artist" -> FavoriteType.ARTIST
                else -> invalidParameter("entityType", "track", "album", "artist")
            }
        }

        getP("/api/favorites/{userId}") {
            val requestingUserId = call.getUserId()
            val userId = call.getLongParameter("userId")

            if (requestingUserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                forbidden(PermissionType.VIEW_USERS)
            }

            val favorites = FavoriteRepo.getFavoriteDtoForUser(userId)

            call.respond(favorites)
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
        }

        deleteP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: unauthorized()
            val place = call.getIntParameter("place")

            if (place !in 1..3)
                invalidParameter("place", "3")

            val entityType = call.getFavoriteType()

            FavoriteRepo.deleteFavoriteAtPlace(userId, entityType, place)

            call.success()
        }

        getP("/api/favorites/{entityType}/check/{entityId}") {

            val userId = call.getUserId() ?: unauthorized()

            val entityId = call.getLongParameter("entityId")

            val entityType = call.getFavoriteType()

            val place = FavoriteRepo.getPlace(userId, entityType, entityId)

            call.respond(mapOf(
                "isFavorite" to (place != null),
                "place" to place
            ))
        }
    }
}