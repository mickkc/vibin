package de.mickkc.vibin.routes

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.users.UserEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.UserRepo
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureUserRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/users", PermissionType.VIEW_USERS) {

            val params = call.getPaginatedSearchParams()

            val (users, count) = UserRepo.getAllUsers(params)
            call.respond(
                PaginatedDto(
                    items = UserRepo.toDto(users),
                    total = count,
                    currentPage = params.page,
                    pageSize = params.pageSize
                )
            )
        }

        getP("/api/users/me") {
            val user = call.getUser() ?: notFound()
            call.respond(UserRepo.toDto(user))
        }

        getP("/api/users/{userId}") {
            val userId = call.getLongParameter("userId")
            val requestingUserId = call.getUserId()
            if (requestingUserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                forbidden(PermissionType.VIEW_USERS)
            }

            val user = UserRepo.getById(userId) ?: notFound()
            call.respond(UserRepo.toDto(user))
        }

        getP("/api/users/username/{username}/exists") {
            val username = call.getStringParameter("username")
            val exists = UserRepo.checkUsernameExists(username)
            call.success(exists)
        }

        postP("/api/users", PermissionType.CREATE_USERS) {
            val userEditDto = call.receive<UserEditDto>()
            val currentUser = call.getUser() ?: unauthorized()

            if (userEditDto.isAdmin != null && userEditDto.isAdmin && !currentUser.isAdmin)
                forbidden()
            if (userEditDto.username == null)
                missingParameter("username")
            else if (UserRepo.checkUsernameExists(userEditDto.username))
                invalidParameter("username")

            val created = UserRepo.updateOrCreateUser(null, userEditDto)!!
            call.respond(UserRepo.toDto(created))
        }

        putP("/api/users/{userId}") {
            val userId = call.getLongParameter("userId")
            val currentUser = call.getUser() ?: unauthorized()
            val user = UserRepo.getById(userId) ?: notFound()

            val userEditDto = call.receive<UserEditDto>()

            if (userEditDto.isAdmin != null && userEditDto.isAdmin != user.isAdmin && !currentUser.isAdmin)
                forbidden()
            if (userId == currentUser.id.value) {
                if (!call.hasPermissions(PermissionType.MANAGE_OWN_USER))
                    forbidden(PermissionType.MANAGE_OWN_USER)
            } else {
                if (!call.hasPermissions(PermissionType.MANAGE_USERS))
                    forbidden(PermissionType.MANAGE_USERS)
            }


            if (userEditDto.username != null && userEditDto.username != user.username) {
                if (UserRepo.checkUsernameExists(userEditDto.username))
                    invalidParameter("username")
            }

            if (userEditDto.password != null) {
                val oldHashedPassword = userEditDto.oldPassword?.let { CryptoUtil.hashPassword(it, user.salt) }
                if (!oldHashedPassword.contentEquals(user.passwordHash) && !currentUser.isAdmin) {
                    forbidden()
                }
            }

            val updated = UserRepo.updateOrCreateUser(user.id.value, userEditDto) ?: notFound()

            call.respond(UserRepo.toDto(updated))
        }

        deleteP("/api/users/{userId}") {
            val userId = call.getLongParameter("userId")
            val deleteData = call.parameters["deleteData"]?.toBoolean() ?: false

            if (userId == call.getUserId()) {
                if (!call.hasPermissions(PermissionType.DELETE_OWN_USER))
                    forbidden(PermissionType.DELETE_OWN_USER)
            }
            else {
                if (!call.hasPermissions(PermissionType.DELETE_USERS))
                    forbidden(PermissionType.DELETE_USERS)
            }

            val deleted = UserRepo.deleteUser(userId, deleteData)

            if (!deleted) {
                notFound()
            }

            call.success()
        }

        getP("/api/users/{userId}/pfp") {
            val requestinguserId = call.getUserId() ?: unauthorized()
            val userId = call.getLongParameter("userId")

            if (requestinguserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                forbidden(PermissionType.VIEW_USERS)
            }

            val quality = call.getIntOrDefault("quality", 0)
            val imageId = UserRepo.getProfilePictureId(userId)

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=user&quality=$quality")
        }
    }
}