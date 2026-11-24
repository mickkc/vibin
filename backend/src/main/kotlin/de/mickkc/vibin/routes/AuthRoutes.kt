package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.SessionsDto
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
import de.mickkc.vibin.db.SessionState
import de.mickkc.vibin.db.TotpType
import de.mickkc.vibin.dto.LoginResultDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.security.TotpManager
import de.mickkc.vibin.utils.RateLimiter
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi
import java.security.MessageDigest

@OptIn(ExperimentalKtorApi::class)
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

        if (user == null) {
            logger.warn("Failed login attempt for non-existent user: $username")
            loginRateLimiter.checkAndIncrement(clientId)
            // Pretend to hash and check the password to prevent time-based username enumeration
            val hashedDummyPassword = CryptoUtil.hashPassword(password, CryptoUtil.getSalt())
            MessageDigest.isEqual(hashedDummyPassword, hashedDummyPassword.reversedArray())
            unauthorized("Invalid username or password")
        }

        val hashedPassword = CryptoUtil.hashPassword(password, user.salt)
        if (!MessageDigest.isEqual(hashedPassword, user.passwordHash)) {
            logger.warn("Failed login attempt for user: $username")
            loginRateLimiter.checkAndIncrement(clientId)
            unauthorized("Invalid username or password")
        }

        val token = CryptoUtil.createToken()

        val state = if (user.totpType == TotpType.None) SessionState.Valid else SessionState.TotpPending
        val session = SessionRepo.addSession(user, token, deviceId, deviceName, state)

        if (state == SessionState.Valid) {
            logger.info("Successful login for user: $username")
            val hashedDeviceId = CryptoUtil.hashPassword(deviceId, user.salt)
            LoginNotificationManager.sendIfNotTrusted(hashedDeviceId, deviceName, call.getClientId(), session, user)
        }

        // Send TOTP code via email if user enabled it
        if (user.totpType == TotpType.Mail) {
            TotpManager.sendTotpEmail(user, session)
        }

        call.respond(LoginResultDto(
            success = true,
            token = session.token,
            user = UserRepo.toDto(user),
            permissions = PermissionRepo.getPermissions(user.id.value).map { it.id },
            totpRequired = state == SessionState.TotpPending
        ))
    }.describe {
        operationId = "login"
        summary = "Login"
        tag("Auth")
        parameters {
            q("username", "mickkc", required = true)
            q("password", "hunter2", required = true)
            q("deviceId", "UM6fjbATKLXiLWSflCeVeGGxRE3L2Aje", required = true,
                description = "An Id identifying this device. Should be persistent across logins from the same device.")
            q("deviceName", "My Laptop", required = true,
                description = "A human-readable description of the device logging in.")
        }
        responses {
            unauthorizedResponse("Invalid username or password")
            rateLimitedResponse()
            invalidParameterResponse("deviceId", "Must be max. 32 characters long.")
        }
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
                permissions = PermissionRepo.getPermissions(user.id.value).map { it.id },
                totpRequired = false
            ))
        }.describe {
            operationId = "validateSession"
            summary = "Validate a session"
            tag("Auth")
            authResponses {
                successResponse<LoginResultDto>("Session is valid")
                unauthorizedResponse("Invalid or expired token")
            }
        }

        // User logout
        postP("/api/auth/logout") {

            val principal = call.principal<UserPrincipal>() ?: unauthorized()
            SessionRepo.removeSession(principal.token)

            logger.info("User ID ${principal.userId} logged out")

            call.success()
        }.describe {
            operationId = "logout"
            summary = "Logout"
            description = "The current token will be invalidated an can no longer be used to access the API."
            tag("Auth")
            authResponses {
                successResponse<SuccessDto>("Logged out")
            }
        }

        getP("/api/auth/sessions", PermissionType.MANAGE_SESSIONS) {

            val userId = call.getUserId() ?: unauthorized()
            val token = call.getToken() ?: unauthorized()

            val sessions = SessionRepo.getAllValidSessionsForUser(userId)

            call.respond(SessionsDto(
                sessions = SessionRepo.toSessionDto(sessions),
                currentSessionIndex = sessions.indexOfFirst { it.token == token }
            ))
        }.describe {
            operationId = "getAllSessions"
            summary = "Get all active sessions for the currently logged-in user"
            tag("Auth")
            authResponses(PermissionType.MANAGE_SESSIONS) {
                successResponse<SessionsDto>("All active sessions and the index of the current session")
            }
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
        }.describe {
            operationId = "invalidateSession"
            summary = "Invalidate a session based on its Id"
            tag("Auth")
            parameters {
                p("id", 1L, description = "The session Id")
            }
            authResponses(PermissionType.MANAGE_SESSIONS) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Session")
                forbiddenResponse("The session does not belong to the user.")
            }
        }

        deleteP("/api/auth/sessions/all", PermissionType.MANAGE_SESSIONS) {

            val userId = call.getUserId() ?: unauthorized()
            val token = call.getToken() ?: unauthorized()

            SessionRepo.invalidateAllOtherSessionsForUser(userId, token)

            call.success()
        }.describe {
            operationId = "invalidateAllSessions"
            summary = "Invalidate all sessions except the current one"
            tag("Auth")
            authResponses(PermissionType.MANAGE_SESSIONS) {
                successResponse<SuccessDto>("Success")
            }
        }
    }
}