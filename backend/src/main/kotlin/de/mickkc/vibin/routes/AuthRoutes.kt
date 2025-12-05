package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.auth.UserPrincipal
import de.mickkc.vibin.dto.LoginResultDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.repos.UserRepo

fun Application.configureAuthRoutes() = routing {

    val logger = LoggerFactory.getLogger("Authentication API Routes")

    // User login
    postP("/api/auth/login") {

        val username = call.getStringParameter("username") ?: return@postP
        val password = call.getStringParameter("password") ?: return@postP

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
            val user = call.getUser() ?: return@postP call.unauthorized()
            val deviceId = call.getStringParameter("deviceId") ?: return@postP

            val mediaToken = SessionRepo.createMediaToken(user, deviceId)

            call.respond(mapOf("mediaToken" to mediaToken.token))
        }

        // Validate media token
        getP("/api/auth/media") {
            val mediaToken = call.getStringParameter("mediaToken") ?: return@getP
            val userId = call.getUserId()

            val user = SessionRepo.getUserFromMediaToken(mediaToken)?.takeIf { it.isActive }
                ?: return@getP call.success(false)

            call.success(user.id.value == userId)
        }

        // Delete media token
        deleteP("/api/auth/media/token") {
            val userId = call.getUserId() ?: return@deleteP call.unauthorized()
            val deviceId = call.getStringParameter("deviceId") ?: return@deleteP

            SessionRepo.deleteMediaToken(userId, deviceId)

            call.success()
        }

        getP("/api/auth/sessions", PermissionType.MANAGE_SESSIONS) {

            val userId = call.getUserId() ?: return@getP call.unauthorized()
            val token = call.getToken() ?: return@getP call.unauthorized()

            val sessions = SessionRepo.getAllSessionsForUser(userId)


            call.respond(mapOf(
                "sessions" to SessionRepo.toSessionDto(sessions),
                "currentSessionIndex" to sessions.indexOfFirst { it.token == token }
            ))
        }

        deleteP("/api/auth/sessions/{id}", PermissionType.MANAGE_SESSIONS) {

            val sessionId = call.getLongParameter("id") ?: return@deleteP

            val userId = call.getUserId()
                ?: return@deleteP call.unauthorized()

            val sessionUserId = SessionRepo.getUserFromSessionId(sessionId)?.id?.value ?: return@deleteP call.notFound()

            if (sessionUserId != userId) {
                return@deleteP call.forbidden()
            }

            SessionRepo.invalidateSessionById(sessionId)

            call.success()
        }

        deleteP("/api/auth/sessions/all", PermissionType.MANAGE_SESSIONS) {

            val deviceId = call.getStringParameter("excludeDeviceId") ?: return@deleteP

            val userId = call.getUserId()
                ?: return@deleteP call.unauthorized()

            val token = call.getToken() ?: return@deleteP call.unauthorized()

            SessionRepo.invalidateAllOtherSessionsForUser(userId, token)
            SessionRepo.invalidateAllOtherMediaTokensForUser(userId, deviceId)

            call.success()
        }
    }
}