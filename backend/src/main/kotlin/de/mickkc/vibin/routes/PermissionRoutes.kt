package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.UserRepo

fun Application.configurePermissionRoutes() = routing {
    authenticate("tokenAuth") {

        get("/api/permissions") {
            val user = call.getUser() ?: return@get call.unauthorized()
            val permissions = if (user.isAdmin) PermissionType.entries else PermissionRepo.getPermissions(user.id.value)
            call.respond(permissions.map { it.id })
        }

        getP("/api/permissions/{userId}", PermissionType.MANAGE_PERMISSIONS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")
            val user = UserRepo.getById(userId) ?: return@getP call.notFound()
            val permissions = PermissionRepo.getPermissions(user.id.value)
            call.respond(permissions.map { it.id })
        }

        putP("/api/permissions/{userId}", PermissionType.MANAGE_PERMISSIONS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@putP call.missingParameter("userId")
            val permissionId = call.request.queryParameters["permissionId"] ?: return@putP call.missingParameter("permissionId")

            val user = UserRepo.getById(userId) ?: return@putP call.notFound()

            val permissionType = PermissionType.valueOfId(permissionId) ?: return@putP call.notFound()
            val hasPermission = PermissionRepo.hasPermissions(user.id.value, listOf(permissionType))

            if (hasPermission) {
                PermissionRepo.removePermission(user.id.value, permissionType)
            } else {
                PermissionRepo.addPermission(user.id.value, permissionType)
            }

            call.respond(mapOf(
                "granted" to !hasPermission,
            ))
        }

    }
}