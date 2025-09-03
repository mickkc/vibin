package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.UserRepo

fun Application.configurePermissionRoutes() = routing {
    authenticate("tokenAuth") {

        get("/api/permissions") {
            val user = call.getUser() ?: return@get call.unauthorized()
            val permissions = if (user.isAdmin) PermissionType.entries else PermissionRepo.getPermissions(user.id.value)
            call.respond(permissions.map { it.id })
        }

        putP("/api/permissions/{userId}", PermissionType.MANAGE_PERMISSIONS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@putP call.missingParameter("userId")
            val permissionId = call.request.queryParameters["permissionId"] ?: return@putP call.missingParameter("permissionId")

            val user = UserRepo.getById(userId) ?: return@putP call.notFound()
            if (user.isAdmin) {
                return@putP call.forbidden("Cannot modify permissions for admin users")
            }

            val permissionType = PermissionType.valueOfId(permissionId) ?: return@putP call.notFound()
            val hasPermission = PermissionRepo.hasPermissions(userId, listOf(permissionType))

            if (hasPermission) {
                PermissionRepo.removePermission(userId, permissionType)
            } else {
                PermissionRepo.addPermission(userId, permissionType)
            }

            call.respond(mapOf(
                "granted" to !hasPermission,
            ))
        }

    }
}