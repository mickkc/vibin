package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.PermissionUpdateDto
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.UserRepo
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configurePermissionRoutes() = routing {
    authenticate("tokenAuth") {

        get("/api/permissions") {
            val user = call.getUser() ?: unauthorized()
            val permissions = if (user.isAdmin) PermissionType.entries else PermissionRepo.getPermissions(user.id.value)
            call.respond(permissions.map { it.id })
        }.describe {
            operationId = "getPermissions"
            summary = "Get permissions for the logged in user"
            tag("Permissions")
            authResponses {
                successResponse<List<String>>("List of permissions for the current user. " +
                        "Admins always have all permissions.",
                    exampleValue = listOf(PermissionType.VIEW_TRACKS.id, PermissionType.STREAM_TRACKS.id, "etc."))
            }
        }

        getP("/api/permissions/{userId}", PermissionType.MANAGE_PERMISSIONS) {
            val userId = call.getLongParameter("userId")
            val user = UserRepo.getById(userId) ?: notFound()
            val permissions = if (user.isAdmin) PermissionType.entries else PermissionRepo.getPermissions(user.id.value)
            call.respond(permissions.map { it.id })
        }.describe {
            operationId = "getPermissionsForUser"
            summary = "Get permissions for another user"
            tag("Permissions")
            authResponses(PermissionType.MANAGE_PERMISSIONS) {
                successResponse<List<String>>("List of permissions for the user. " +
                        "Admins always have all permissions.",
                    exampleValue = listOf(PermissionType.VIEW_TRACKS.id, PermissionType.STREAM_TRACKS.id, "etc."))
            }
        }

        putP("/api/permissions/{userId}", PermissionType.MANAGE_PERMISSIONS) {
            val userId = call.getLongParameter("userId")
            val permissionId = call.getStringParameter("permissionId")

            val user = UserRepo.getById(userId) ?: notFound()

            val permissionType = PermissionType.valueOfId(permissionId) ?: notFound()
            val hasPermission = PermissionRepo.hasPermissions(user.id.value, listOf(permissionType))

            if (hasPermission) {
                PermissionRepo.removePermission(user.id.value, permissionType)
            } else {
                PermissionRepo.addPermission(user.id.value, permissionType)
            }

            call.respond(PermissionUpdateDto(granted = !hasPermission))
        }.describe {
            operationId = "togglePermission"
            summary = "Toggle a permission for a user"
            tag("Permissions")
            parameters {
                p("userId", 1L)
                q("permissionId", PermissionType.DELETE_ARTISTS.id, required = true)
            }
            authResponses(PermissionType.MANAGE_PERMISSIONS) {
                successResponse<PermissionUpdateDto>("New permission status")
                notFoundResponse("User or permission")
            }
        }

    }
}