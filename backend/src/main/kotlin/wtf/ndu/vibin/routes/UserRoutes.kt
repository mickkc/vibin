package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.users.UserEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.UserRepo
import wtf.ndu.vibin.utils.ImageUtils

fun Application.configureUserRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/users", PermissionType.VIEW_USERS) {

            val params = call.getPaginatedSearchParams() ?: return@getP

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

        getP("/api/users/me", PermissionType.VIEW_USERS) {
            val user = call.getUser() ?: return@getP call.notFound()
            call.respond(UserRepo.toDto(user))
        }

        getP("/api/users/{userId}", PermissionType.VIEW_USERS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")
            val user = UserRepo.getById(userId) ?: return@getP call.notFound()
            call.respond(UserRepo.toDto(user))
        }

        getP("/api/users/username/{username}/exists") {
            val username = call.parameters["username"] ?: return@getP call.missingParameter("username")
            val exists = UserRepo.checkUsernameExists(username)
            call.success(exists)
        }

        postP("/api/users", PermissionType.CREATE_USERS) {
            val userEditDto = call.receive<UserEditDto>()

            if (userEditDto.username == null)
                return@postP call.missingParameter("username")
            else if (UserRepo.checkUsernameExists(userEditDto.username))
                return@postP call.invalidParameter("username")

            val created = UserRepo.updateOrCreateUser(null, userEditDto)!!
            call.respond(UserRepo.toDto(created))
        }

        putP("/api/users/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@putP call.missingParameter("userId")
            val currentUser = call.getUser() ?: return@putP call.unauthorized()

            if (userId == currentUser.id.value) {
                if (!call.hasPermissions(PermissionType.MANAGE_OWN_USER))
                    return@putP call.forbidden(PermissionType.MANAGE_OWN_USER)
            } else {
                if (!call.hasPermissions(PermissionType.MANAGE_USERS))
                    return@putP call.forbidden(PermissionType.MANAGE_USERS)
            }

            val user = UserRepo.getById(userId) ?: return@putP call.notFound()

            val userEditDto = call.receive<UserEditDto>()

            if (userEditDto.username != null && userEditDto.username != user.username) {
                if (UserRepo.checkUsernameExists(userEditDto.username))
                    return@putP call.invalidParameter("username")
            }

            if (userEditDto.password != null) {
                val oldHashedPassword = userEditDto.password.let { CryptoUtil.hashPassword(it, user.salt) }
                if (!oldHashedPassword.contentEquals(user.passwordHash) && !currentUser.isAdmin) {
                    return@putP call.forbidden()
                }
            }

            val updated = UserRepo.updateOrCreateUser(user.id.value, userEditDto) ?: return@putP call.notFound()

            call.respond(UserRepo.toDto(updated))
        }

        deleteP("/api/users/{userId}") {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("userId")
            val deleteData = call.parameters["deleteData"]?.toBoolean() ?: false

            if (userId == call.getUserId()) {
                if (!call.hasPermissions(PermissionType.DELETE_OWN_USER))
                    return@deleteP call.forbidden(PermissionType.DELETE_OWN_USER)
            }
            else {
                if (!call.hasPermissions(PermissionType.DELETE_USERS))
                    return@deleteP call.forbidden(PermissionType.DELETE_USERS)
            }

            val deleted = UserRepo.deleteUser(userId, deleteData)

            if (!deleted) {
                return@deleteP call.notFound()
            }

            call.success()
        }

        getP("/api/users/{userId}/pfp", PermissionType.VIEW_USERS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")
            val user = UserRepo.getById(userId) ?: return@getP call.notFound()
            val quality = call.request.queryParameters["quality"]?.toIntOrNull() ?: 0
            val profilePicture = UserRepo.getProfilePicture(user)

            val file = ImageUtils.getFileOrDefault(profilePicture, quality, "user") ?: return@getP call.notFound()

            call.respondFile(file)
        }
    }
}