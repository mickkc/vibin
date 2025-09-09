package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.dto.users.UserEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.UserRepo

fun Application.configureUserRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/users", PermissionType.VIEW_USERS) {
            val users = UserRepo.getAllUsers()
            call.respond(UserRepo.toDto(users))
        }

        getP("/api/users/{userId}", PermissionType.VIEW_USERS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@getP call.missingParameter("userId")
            val user = UserRepo.getById(userId) ?: return@getP call.notFound()
            call.respond(UserRepo.toDto(user))
        }

        postP("/api/users", PermissionType.CREATE_USERS) {
            val userEditDto = call.receive<UserEditDto>()

            if (userEditDto.username == null)
                return@postP call.missingParameter("username")

            val created = UserRepo.updateOrCreateUser(null, userEditDto)!!
            call.respond(UserRepo.toDto(created))
        }

        putP("/api/users/{userId}", PermissionType.MANAGE_USERS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@putP call.missingParameter("userId")
            val user = UserRepo.getById(userId) ?: return@putP call.notFound()

            val userEditDto = call.receive<UserEditDto>()
            val updated = UserRepo.updateOrCreateUser(user.id.value, userEditDto) ?: return@putP call.notFound()

            call.respond(UserRepo.toDto(updated))
        }

        deleteP("/api/users/{userId}", PermissionType.DELETE_USERS) {
            val userId = call.parameters["userId"]?.toLongOrNull() ?: return@deleteP call.missingParameter("userId")
            val user = UserRepo.getById(userId) ?: return@deleteP call.notFound()
            UserRepo.deleteUser(user)
            call.success()
        }
    }
}