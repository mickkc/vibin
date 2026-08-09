package de.mickkc.vibin.routes

import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.security.exception.LoginNotificationAlreadyHandledException
import de.mickkc.vibin.security.exception.LoginNotificationExpiredException
import de.mickkc.vibin.templates.BaseTemplate
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.templates.security.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Application.configureSecurityRoutes() = routing {

    val logger: Logger = LoggerFactory.getLogger("Security API Routes")

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
        val token = call.getStringParameter("tk") ?: return@get

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
        val token = call.getStringParameter("tk") ?: return@get

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
}