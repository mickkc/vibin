package wtf.ndu.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.auth.UserPrincipal
import wtf.ndu.vibin.repos.SessionRepo
import wtf.ndu.vibin.repos.UserRepo

fun Application.configureAuthRoutes() = routing {

    val logger = LoggerFactory.getLogger("Authentication API Routes")

    // User login
    post("/api/auth/login") {

        val username = call.parameters["username"] ?: return@post call.missingParameter("username")
        val password = call.parameters["password"] ?: return@post call.missingParameter("password")

        val user = UserRepo.getByUsername(username)?.takeIf { it.isActive }
            ?: return@post call.unauthorized("Invalid username or password")

        val hashedPassword = CryptoUtil.hashPassword(password, user.salt)
        if (!hashedPassword.contentEquals(user.passwordHash)) {
            logger.warn("Failed login attempt for user: $username")
            return@post call.unauthorized("Invalid username or password")
        }

        val token = CryptoUtil.createToken()
        val session = SessionRepo.addSession(user, token)

        logger.info("Successful login for user: $username")

        call.respond(mapOf(
            "success" to true,
            "token" to session.token,
            "user" to UserRepo.toDto(user)
        ))
    }

    authenticate("tokenAuth") {

        // Session validation
        post("/api/auth/validate") {
            val user = call.getUser()?.takeIf { it.isActive }
                ?: return@post call.unauthorized("Invalid or expired token")

            call.respond(mapOf(
                "success" to true,
                "user" to UserRepo.toDto(user)
            ))
        }

        // User logout
        post("/api/auth/logout") {

            val principal = call.principal<UserPrincipal>() ?: return@post call.unauthorized()
            SessionRepo.removeSession(principal.token)

            logger.info("User ID ${principal.userId} logged out")

            call.respond(mapOf("success" to true))
        }

        // Change password
        post("/api/auth/password") {
            val user = call.getUser() ?: return@post call.unauthorized()

            val currentPassword = call.parameters["currentPassword"] ?: return@post call.missingParameter("currentPassword")
            val newPassword = call.parameters["newPassword"] ?: return@post call.missingParameter("newPassword")

            val currentHashedPassword = CryptoUtil.hashPassword(currentPassword, user.salt)
            if (!currentHashedPassword.contentEquals(user.passwordHash)) {
                return@post call.unauthorized("Invalid current password")
            }

            val newSalt = CryptoUtil.getSalt()
            val newHashedPassword = CryptoUtil.hashPassword(newPassword, newSalt)

            UserRepo.updateUser(user) {
                passwordHash = newHashedPassword
                salt = newSalt
            }

            logger.info("User ID ${user.id.value} changed their password")

            call.respond(mapOf("success" to true))
        }
    }
}