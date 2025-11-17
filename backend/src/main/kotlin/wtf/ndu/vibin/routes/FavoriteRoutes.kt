package wtf.ndu.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import wtf.ndu.vibin.db.FavoriteType
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.FavoriteRepo

fun Application.configureFavoriteRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/favorites/{userId}", PermissionType.VIEW_USERS) {

            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")

            val favorites = FavoriteRepo.getFavoriteDtoForUser(userId)

            call.respond(favorites)
        }

        putP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: return@putP call.unauthorized()
            val entityTypeParam = call.parameters["entityType"] ?: return@putP call.missingParameter("entityType")
            val entityId = call.parameters["entityId"]?.toLongOrNull() ?: return@putP call.missingParameter("entityId")
            val place = call.parameters["place"]?.toIntOrNull() ?: return@putP call.missingParameter("place")

            if (place !in 1..3)
                return@putP call.invalidParameter("place", "1", "2", "3")

            val entityType = when (entityTypeParam.lowercase()) {
                "track" -> FavoriteType.TRACK
                "album" -> FavoriteType.ALBUM
                "artist" -> FavoriteType.ARTIST
                else -> return@putP call.invalidParameter("entityType", "track", "album", "artist")
            }

            FavoriteRepo.addFavorite(userId, entityType, entityId, place)

            call.success()
        }

        deleteP("/api/favorites/{entityType}/{place}", PermissionType.MANAGE_OWN_USER) {

            val userId = call.getUserId() ?: return@deleteP call.unauthorized()
            val entityTypeParam = call.parameters["entityType"] ?: return@deleteP call.missingParameter("entityType")
            val place = call.parameters["place"]?.toIntOrNull() ?: return@deleteP call.missingParameter("place")

            if (place !in 1..3)
                return@deleteP call.invalidParameter("place", "1", "2", "3")

            val entityType = when (entityTypeParam.lowercase()) {
                "track" -> FavoriteType.TRACK
                "album" -> FavoriteType.ALBUM
                "artist" -> FavoriteType.ARTIST
                else -> return@deleteP call.invalidParameter("entityType", "track", "album", "artist")
            }

            FavoriteRepo.deleteFavoriteAtPlace(userId, entityType, place)

            call.success()
        }
    }
}