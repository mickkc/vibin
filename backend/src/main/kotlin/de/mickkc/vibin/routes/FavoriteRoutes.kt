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

        suspend fun RoutingCall.getFavoriteType(): FavoriteType? {
            val entityTypeParam = this.parameters["entityType"] ?: return null

            return when (entityTypeParam.lowercase()) {
                "track" -> FavoriteType.TRACK
                "album" -> FavoriteType.ALBUM
                "artist" -> FavoriteType.ARTIST
                else -> null.also {
                    this.invalidParameter("entityType", "track", "album", "artist")
                }
            }
        }

        getP("/api/favorites/{userId}", PermissionType.VIEW_USERS) {

            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")

            val favorites = FavoriteRepo.getFavoriteDtoForUser(userId)

            call.respond(favorites)
        }

        putP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: return@putP call.unauthorized()
            val entityId = call.parameters["entityId"]?.toLongOrNull() ?: return@putP call.missingParameter("entityId")
            val place = call.parameters["place"]?.toIntOrNull() ?: return@putP call.missingParameter("place")

            if (place !in 1..3)
                return@putP call.invalidParameter("place", "1", "2", "3")

            val entityType = call.getFavoriteType() ?: return@putP

            FavoriteRepo.addFavorite(userId, entityType, entityId, place)

            call.success()
        }

        deleteP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: return@deleteP call.unauthorized()
            val place = call.parameters["place"]?.toIntOrNull() ?: return@deleteP call.missingParameter("place")

            if (place !in 1..3)
                return@deleteP call.invalidParameter("place", "1", "2", "3")

            val entityType = call.getFavoriteType() ?: return@deleteP

            FavoriteRepo.deleteFavoriteAtPlace(userId, entityType, place)

            call.success()
        }

        getP("/api/favorites/{entityType}/check/{entityId}", PermissionType.VIEW_USERS) {

            val userId = call.getUserId() ?: return@getP call.unauthorized()

            val entityId = call.parameters["entityId"]?.toLongOrNull() ?: return@getP call.missingParameter("entityId")

            val entityType = call.getFavoriteType() ?: return@getP

            val place = FavoriteRepo.getPlace(userId, entityType, entityId)

            call.respond(mapOf(
                "isFavorite" to (place != null),
                "place" to place
            ))
        }
    }
}