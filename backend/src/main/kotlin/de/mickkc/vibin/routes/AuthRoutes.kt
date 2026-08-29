package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import de.mickkc.vibin.auth.CryptoUtil
import de.mickkc.vibin.auth.UserPrincipal
import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS
import de.mickkc.vibin.config.EnvUtil.LOGIN_RATE_LIMIT_TIMEOUT
import de.mickkc.vibin.dto.LoginResultDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.utils.RateLimiter

fun Application.configureAuthRoutes() = routing {

    val logger = LoggerFactory.getLogger("Authentication API Routes")
    val loginRateLimiter = RateLimiter(
        maxRequests = EnvUtil.getOrError(LOGIN_RATE_LIMIT_ATTEMPTS),
        windowMinutes = EnvUtil.getOrError(LOGIN_RATE_LIMIT_TIMEOUT)
    )

    // User login
    postP("/api/auth/login") {

        val clientId = call.getClientId()

        if (!loginRateLimiter.check(clientId)) {
            rateLimitExceeded()
        }

        val username = call.getStringParameter("username")
        val password = call.getStringParameter("password")
        val deviceId = call.getStringParameter("deviceId")
        val deviceName = call.getStringParameter("deviceName").take(255)

        if (deviceId.length > 32) {
            invalidParameter("deviceId", "deviceId.length <= 32")
        }

        val user = UserRepo.getByUsername(username)?.takeIf { it.isActive }
            ?: unauthorized("Invalid username or password")

        val hashedPassword = CryptoUtil.hashPassword(password, user.salt)
        if (!hashedPassword.contentEquals(user.passwordHash)) {
            logger.warn("Failed login attempt for user: $username")
            loginRateLimiter.checkAndIncrement(clientId)
            unauthorized("Invalid username or password")
        }

        val token = CryptoUtil.createToken()
        val session = SessionRepo.addSession(user, token, deviceId, deviceName)

        logger.info("Successful login for user: $username")
        LoginNotificationManager.sendIfNotTrusted(deviceId, deviceName, call.getClientId(), session, user)

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
                ?: unauthorized("Invalid or expired token")

            call.respond(LoginResultDto(
                success = true,
                token = call.getToken()!!,
                user = UserRepo.toDto(user),
                permissions = PermissionRepo.getPermissions(user.id.value).map { it.id }
            ))
        }

        // User logout
        postP("/api/auth/logout") {

            val principal = call.principal<UserPrincipal>() ?: unauthorized()
            SessionRepo.removeSession(principal.token)

            logger.info("User ID ${principal.userId} logged out")

            call.success()
        }

        getP("/api/auth/sessions", PermissionType.MANAGE_SESSIONS) {

            val userId = call.getUserId() ?: unauthorized()
            val token = call.getToken() ?: unauthorized()

            val sessions = SessionRepo.getAllSessionsForUser(userId)


            call.respond(mapOf(
                "sessions" to SessionRepo.toSessionDto(sessions),
                "currentSessionIndex" to sessions.indexOfFirst { it.token == token }
            ))
        }

        deleteP("/api/auth/sessions/{id}", PermissionType.MANAGE_SESSIONS) {

            val sessionId = call.getLongParameter("id")

            val userId = call.getUserId()
                ?: unauthorized()

            val sessionUserId = SessionRepo.getUserFromSessionId(sessionId)?.id?.value ?: notFound()

            if (sessionUserId != userId) {
                forbidden()
            }

            SessionRepo.invalidateSessionById(sessionId)

            call.success()
        }

        deleteP("/api/auth/sessions/all", PermissionType.MANAGE_SESSIONS) {

            val userId = call.getUserId()
                ?: unauthorized()

            val token = call.getToken() ?: unauthorized()

            SessionRepo.invalidateAllOtherSessionsForUser(userId, token)

            call.success()
        }
    }
}