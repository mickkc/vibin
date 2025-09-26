package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.auth.UserPrincipal
import wtf.ndu.vibin.dto.LoginResultDto
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.SessionRepo
import wtf.ndu.vibin.repos.UserRepo

fun Application.configureAuthRoutes() = routing {

    val logger = LoggerFactory.getLogger("Authentication API Routes")

    // User login
    postP("/api/auth/login") {

        val username = call.parameters["username"] ?: return@postP call.missingParameter("username")
        val password = call.parameters["password"] ?: return@postP call.missingParameter("password")

        val user = UserRepo.getByUsername(username)?.takeIf { it.isActive }
            ?: return@postP call.unauthorized("Invalid username or password")

        val hashedPassword = CryptoUtil.hashPassword(password, user.salt)
        if (!hashedPassword.contentEquals(user.passwordHash)) {
            logger.warn("Failed login attempt for user: $username")
            return@postP call.unauthorized("Invalid username or password")
        }

        val token = CryptoUtil.createToken()
        val session = SessionRepo.addSession(user, token)

        logger.info("Successful login for user: $username")

        call.respond(LoginResultDto(
            success = true,
            token = session.token,
            user = UserRepo.toDto(user),
            permissions = PermissionRepo.getPermissions(user.id.value).map { it.id }
        ))
    }

    authenticate("tokenAuth") {

        // Session validation
        postP("/api/auth/validate") {
            val user = call.getUser()?.takeIf { it.isActive }
                ?: return@postP call.unauthorized("Invalid or expired token")

            call.respond(LoginResultDto(
                success = true,
                token = call.getToken()!!,
                user = UserRepo.toDto(user),
                permissions = PermissionRepo.getPermissions(user.id.value).map { it.id }
            ))
        }

        // User logout
        postP("/api/auth/logout") {

            val principal = call.principal<UserPrincipal>() ?: return@postP call.unauthorized()
            SessionRepo.removeSession(principal.token)

            logger.info("User ID ${principal.userId} logged out")

            call.success()
        }

        // Create new media token
        postP("/api/auth/media/token") {
            val user = call.getUser()
                ?: return@postP call.unauthorized()
            val deviceId = call.request.queryParameters["deviceId"]
                ?: return@postP call.missingParameter("deviceId")

            val mediaToken = SessionRepo.createMediaToken(user, deviceId)

            call.respond(mapOf("mediaToken" to mediaToken.token))
        }

        // Validate media token
        getP("/api/auth/media") {
            val mediaToken = call.request.queryParameters["mediaToken"]
                ?: return@getP call.missingParameter("mediaToken")
            val userId = call.getUserId()

            val user = SessionRepo.getUserFromMediaToken(mediaToken)?.takeIf { it.isActive }
                ?: return@getP call.success(false)

            call.success(user.id.value == userId)
        }

        // Delete media token
        deleteP("/api/auth/media/token") {
            val userId = call.getUserId()
                ?: return@deleteP call.unauthorized()
            val deviceId = call.request.queryParameters["deviceId"]
                ?: return@deleteP call.missingParameter("deviceId")

            SessionRepo.deleteMediaToken(userId, deviceId)

            call.success()
        }
    }
}