package de.mickkc.vibin.routes

import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.users.UserDto
import de.mickkc.vibin.dto.users.UserEditDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.security.MailVerificationManager
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
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
        }.describe {
            operationId = "getAllUsers"
            summary = "Get all users"
            tag("Users")
            parameters {
                paginatedSearch()
            }
            authResponses(PermissionType.VIEW_USERS) {
                successResponse<PaginatedDto<UserDto>>("A paginated list of users")
            }
        }

        getP("/api/users/me") {
            val user = call.getUser() ?: notFound()
            call.respond(UserRepo.toDto(user))
        }.describe {
            operationId = "getCurrentUser"
            summary = "Get the currently logged-in user"
            tag("Users")
            authResponses {
                successResponse<UserDto>("The currently logged-in user")
            }
        }

        getP("/api/users/{userId}") {
            val userId = call.getLongParameter("userId")
            val requestingUserId = call.getUserId()
            if (requestingUserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                noPermission(PermissionType.VIEW_USERS)
            }

            val user = UserRepo.getById(userId) ?: notFound()
            call.respond(UserRepo.toDto(user))
        }.describe {
            operationId = "getUserById"
            summary = "Get a user by their Id"
            tag("Users")
            parameters {
                p("userId", 1L)
            }
            authResponses {
                successResponse<UserDto>("The user")
                notFoundResponse("User")
                missingPermissionsResponse(PermissionType.VIEW_USERS)
            }
            optionalPermissions(
                PermissionType.VIEW_USERS to "Request users other than yourself"
            )
        }

        getP("/api/users/username/{username}/exists", PermissionType.VIEW_USERS) {
            val username = call.getStringParameter("username")
            val exists = UserRepo.checkUsernameExists(username)
            call.success(exists)
        }.describe {
            operationId = "checkUsername"
            summary = "Check if a username is already taken"
            description = "Usernames are case-insensitive"
            tag("Users")
            parameters {
                p("username", "mickkc")
            }
            authResponses(PermissionType.VIEW_USERS) {
                successResponse<SuccessDto>("True if the username is taken, false if not")
            }
        }

        postP("/api/users", PermissionType.CREATE_USERS) {
            val userEditDto = call.receive<UserEditDto>()
            userEditDto.validate()

            val currentUser = call.getUser() ?: unauthorized()

            if (userEditDto.isAdmin != null && userEditDto.isAdmin && !currentUser.isAdmin)
                forbidden()
            if (userEditDto.username == null)
                missingParameter("username")
            else if (UserRepo.checkUsernameExists(userEditDto.username))
                invalidParameter("username")

            val created = UserRepo.updateOrCreateUser(null, userEditDto)!!
            if (created.shouldVerifyEmail) {
                MailVerificationManager.sendVerificationMail(created.user)
            }

            call.respond(UserRepo.toDto(created.user))
        }.describe {
            operationId = "createUser"
            summary = "Create a new user"
            description = "When SMTP is configured an there is an email address specified, a verification mail will be " +
                    "sent to that address."
            tag("Users")
            requestBody {
                description = "The user edit data. At least the username and password must be non-null."
                schema = jsonSchema<UserEditDto>()
            }
            authResponses(PermissionType.CREATE_USERS) {
                successResponse<UserDto>("The created user")
                forbiddenResponse("Non-Admin users can't create admin users.")
                invalidParameterResponse("username/displayName/email",
                    "The username/displayName/email is longer than 255 characters / the username is already taken")
            }
        }

        putP("/api/users/{userId}") {
            val userId = call.getLongParameter("userId")
            val currentUser = call.getUser() ?: unauthorized()
            val user = UserRepo.getById(userId) ?: notFound()

            val userEditDto = call.receive<UserEditDto>()
            userEditDto.validate()

            if (userEditDto.isAdmin != null && userEditDto.isAdmin != user.isAdmin && !currentUser.isAdmin)
                forbidden()
            if (userId == currentUser.id.value) {
                if (!call.hasPermissions(PermissionType.MANAGE_OWN_USER))
                    noPermission(PermissionType.MANAGE_OWN_USER)
            } else {
                if (!call.hasPermissions(PermissionType.MANAGE_USERS))
                    noPermission(PermissionType.MANAGE_USERS)
            }


            if (userEditDto.username != null && !userEditDto.username.equals(user.username, true)) {
                if (UserRepo.checkUsernameExists(userEditDto.username))
                    invalidParameter("username")
            }

            if (userEditDto.password != null) {
                val oldHashedPassword = userEditDto.oldPassword?.let { CryptoUtil.hashPassword(it, user.salt) }
                if (!oldHashedPassword.contentEquals(user.passwordHash) && !currentUser.isAdmin) {
                    forbidden("Old password is incorrect")
                }
            }

            val updated = UserRepo.updateOrCreateUser(user.id.value, userEditDto) ?: notFound()
            if (updated.shouldVerifyEmail) {
                MailVerificationManager.sendVerificationMail(updated.user)
            }

            call.respond(UserRepo.toDto(updated.user))
        }.describe {
            operationId = "updateUser"
            summary = "Update a user"
            description = "When SMTP is configured an there is an email address specified, a verification mail will be " +
                    "sent to that address."
            tag("Users")
            parameters {
                p("userId", 1L)
            }
            requestBody {
                description = "The user edit data. Null values will not be updated."
                schema = jsonSchema<UserEditDto>()
            }
            authResponses(PermissionType.MANAGE_USERS) {
                successResponse<UserDto>("The updated user")
                forbiddenResponse("Non-Admin users can't create admin users. / The old password is incorrect.")
                invalidParameterResponse("username/displayName/email",
                    "The username/displayName/email is longer than 255 characters / the username is already taken")
                notFoundResponse("User")
            }
        }

        deleteP("/api/users/{userId}") {
            val userId = call.getLongParameter("userId")
            val deleteData = call.parameters["deleteData"]?.toBoolean() ?: false

            if (userId == call.getUserId()) {
                if (!call.hasPermissions(PermissionType.DELETE_OWN_USER))
                    noPermission(PermissionType.DELETE_OWN_USER)
            }
            else {
                if (!call.hasPermissions(PermissionType.DELETE_USERS))
                    noPermission(PermissionType.DELETE_USERS)
            }

            val deleted = UserRepo.deleteUser(userId, deleteData)

            if (!deleted) {
                notFound()
            }

            call.success()
        }.describe {
            operationId = "deleteUser"
            summary = "Delete a user"
            tag("Users")
            parameters {
                p("userId", 1L)
            }
            authResponses(PermissionType.MANAGE_USERS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("User")
            }
        }

        getP("/api/users/{userId}/pfp") {
            val requestingUserId = call.getUserId() ?: unauthorized()
            val userId = call.getLongParameter("userId")

            if (requestingUserId != userId && !call.hasPermissions(PermissionType.VIEW_USERS)) {
                noPermission(PermissionType.VIEW_USERS)
            }

            val quality = call.getIntOrDefault("quality", 0)
            val imageId = UserRepo.getProfilePictureId(userId)

            call.respondRedirect("/api/images/${if (imageId != null) "by-id/${imageId}" else "default" }?type=user&quality=$quality")
        }.describe {
            operationId = "getUserProfilePicture"
            summary = "Get a users profile picture"
            description = "Redirects to the profile picture of a user, or the default placeholder if the user does not have a profile picture"
            tag("Users")
            parameters {
                p("userId", 1L)
                q("quality", 0, required = false,
                    description = "The width and height of the image. 0 means the original size.")
            }
            authResponses(PermissionType.VIEW_USERS) {
                redirectResponse("`/api/images/by-id/{imageId}` or `/api/images/default`")
                notFoundResponse("User")
            }
        }
    }
}