package de.mickkc.vibin.routes

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS
import de.mickkc.vibin.config.EnvUtil.LOGIN_RATE_LIMIT_TIMEOUT
import de.mickkc.vibin.db.TotpType
import de.mickkc.vibin.dto.TotpStatusDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.security.TotpManager
import de.mickkc.vibin.security.exception.LoginNotificationAlreadyHandledException
import de.mickkc.vibin.security.exception.LoginNotificationExpiredException
import de.mickkc.vibin.templates.BaseTemplate
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.templates.security.*
import de.mickkc.vibin.utils.RateLimiter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Application.configureSecurityRoutes() = routing {

    val logger: Logger = LoggerFactory.getLogger("Security API Routes")

    val totpRateLimiter = RateLimiter(
        maxRequests = EnvUtil.getOrError(LOGIN_RATE_LIMIT_ATTEMPTS),
        windowMinutes = EnvUtil.getOrError(LOGIN_RATE_LIMIT_TIMEOUT)
    )

    staticResources("/api/web", "web.static")

    suspend fun ApplicationCall.invalidNotificationToken() {
        respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.invalid_token.title"))) {
            content {
                insert(InvalidTokenTemplate()) {}
            }
        }
    }

    suspend fun ApplicationCall.processDone() {
        respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.done.title"))) {
            content {
                insert(DoneTemplate()) {}
            }
        }
    }

    get("/api/mail/reset-password") {
        val token = call.getStringParameter("tk")

        if (!LoginNotificationManager.isLoginNotificationTokenValid(token)) {
            return@get call.invalidNotificationToken()
        }

        call.respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.reset_password.title"))) {
            content {
                insert(ResetPasswordTemplate(token)) {}
            }
        }
    }

    get("/api/mail/trust-device") {
        val token = call.getStringParameter("tk")

        if (!LoginNotificationManager.isLoginNotificationTokenValid(token)) {
            return@get call.invalidNotificationToken()
        }

        call.respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.trust_device.title"))) {
            content {
                insert(TrustDeviceTemplate(token)) {}
            }
        }
    }

    suspend fun ApplicationCall.returnHtmlError(reasonKey: String) {
        respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.error.title"))) {
            content {
                insert(ErrorTemplate(reasonKey, request.headers["Referer"])) {}
            }
        }
    }

    post("/api/mail/reset-password") {
        val parameters = call.receiveParameters()

        val token = parameters["token"] ?: return@post call.returnHtmlError("web.security.error.missing_token")
        val newPassword = parameters["newPassword"] ?: return@post call.returnHtmlError("web.security.error.missing_password")
        val confirmNewPassword = parameters["confirmNewPassword"] ?: return@post call.returnHtmlError("web.security.error.missing_confirm_password")

        if (newPassword != confirmNewPassword) {
            return@post call.returnHtmlError("web.security.error.passwords_dont_match")
        }

        if (newPassword.length < 8) {
            return@post call.returnHtmlError("web.security.error.password_too_short")
        }

        try {
            LoginNotificationManager.changePasswordFromLoginEmail(token, newPassword)
        }
        catch (e: Exception) {
            logger.error("An error occurred while trying to reset a password.", e)

            if (e is LoginNotificationExpiredException || e is LoginNotificationAlreadyHandledException) {
                return@post call.invalidNotificationToken()
            }

            return@post call.returnHtmlError("web.security.error.server_error")
        }

        call.processDone()
    }

    post("/api/mail/trust-device") {
        val parameters = call.receiveParameters()

        val token = parameters["token"] ?: return@post call.returnHtmlError("web.security.error.missing_token")

        try {
            LoginNotificationManager.trustDeviceIdFromLoginEmail(token)
        }
        catch (e: Exception) {
            logger.error("An error occurred while trying to trust a device.", e)

            if (e is LoginNotificationExpiredException || e is LoginNotificationAlreadyHandledException) {
                return@post call.invalidNotificationToken()
            }

            return@post call.returnHtmlError("web.security.error.server_error")
        }

        call.processDone()
    }

    authenticate("tokenAuth") {
        getP("/api/auth/totp/qr", PermissionType.MANAGE_OWN_2FA) {
            try {
                val user = call.getUser() ?: unauthorized()
                call.respondBytes(TotpManager.getQrCodeForUser(user), ContentType.Image.PNG)
            }
            catch (e: IllegalStateException) {
                logger.error(e)
                forbidden()
            }
        }

        put("/api/auth/totp") {
        putP("/api/auth/totp", PermissionType.MANAGE_OWN_2FA) {
            val user = call.getUser() ?: unauthorized()
            val type = call.getEnum<TotpType>("type", null)
            when (type) {
                TotpType.None -> TotpManager.disableTotp(user)
                TotpType.Mail -> TotpManager.enableEmailTotp(user)
                TotpType.Totp -> {
                    val code = call.getStringParameter("code")
                    if (!TotpManager.enableTotp(user, code)) {
                        invalidParameter("code")
                    }
                }
            }

            call.success()
        }

    }

    post("/api/auth/totp") {
        val token = call.getStringParameter("token")
        val code = call.getStringParameter("code")

        val session = SessionRepo.getFromSessionToken(token) ?: unauthorized()
        val user = SessionRepo.getUser(session)

        if (!PermissionRepo.hasPermission(user.id.value, PermissionType.MANAGE_OWN_2FA)) {
            forbidden(PermissionType.MANAGE_OWN_2FA)
        }

        if (!totpRateLimiter.check(call.getClientId())) {
            rateLimitExceeded()
        }

        when (user.totpType) {
            TotpType.Totp -> {
                if (!TotpManager.validateTotp(user, code)) {
                    totpRateLimiter.checkAndIncrement(call.getClientId())
                    invalidParameter("code")
                }
                else {
                    if (SessionRepo.setValidTotp(token)) {
                        LoginNotificationManager.sendIfNotTrusted(session, call.getClientId())
                        return@post call.success()
                    }
                }
            }
            TotpType.Mail -> {
                val session = SecurityRepo.getSessionFromTotpMailCode(token, code)
                if (session == null) {
                    totpRateLimiter.checkAndIncrement(call.getClientId())
                    invalidParameter("code")
                }

                if (SessionRepo.setValidTotp(session.token)) {
                    LoginNotificationManager.sendIfNotTrusted(session, call.getClientId())
                    SecurityRepo.removeTotpMailCode(session.id.value)
                    return@post call.success()
                }
            }
            TotpType.None -> notFound()
        }

        notFound()
    }
}