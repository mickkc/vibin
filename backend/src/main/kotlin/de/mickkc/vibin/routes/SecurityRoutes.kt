package de.mickkc.vibin.routes

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS
import de.mickkc.vibin.config.EnvUtil.LOGIN_RATE_LIMIT_TIMEOUT
import de.mickkc.vibin.db.TotpType
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.TotpStatusDto
import de.mickkc.vibin.mail.MailSender
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PermissionRepo
import de.mickkc.vibin.repos.SecurityRepo
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.security.MailVerificationManager
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
import io.ktor.server.routing.openapi.describe
import io.ktor.util.logging.*
import io.ktor.utils.io.ExperimentalKtorApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalKtorApi::class)
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

    suspend fun ApplicationCall.invalidVerificationToken() {
        respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.invalid_token.title"))) {
            content {
                insert(InvalidVerificationTokenTemplate()) {}
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
    }.describe {
        operationId = "getResetPasswordHTML"
        summary = "[HTML] Password reset form"
        tag("Security")
        parameters {
            p("tk", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM",
                description = "The token from the login notification email")
        }
        responses {
            successResponse<String>("Password reset view or an error view, in case the token is invalid.")
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
    }.describe {
        operationId = "getTrustDeviceHTML"
        summary = "[HTML] Trust device form"
        tag("Security")
        parameters {
            p("tk", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM",
                description = "The token from the login notification email")
        }
        responses {
            successResponse<String>("Trust device view or an error view, in case the token is invalid.")
        }
    }

    get("/api/mail/verify") {
        val token = call.getStringParameter("tk")

        val emailAddress = SecurityRepo.getMailAddressFromVerificationToken(token)
            ?: return@get call.invalidVerificationToken()

        call.respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.verify_email.title"))) {
            content {
                insert(VerifyEmailAddressTemplate(token, emailAddress)) {}
            }
        }
    }.describe {
        operationId = "getMailVerificationHTML"
        summary = "[HTML] Mail verification form"
        tag("Security")
        parameters {
            p("tk", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM",
                description = "The token from the verification mail")
        }
        responses {
            successResponse<String>("Mail verification view or an error view, in case the token is invalid.")
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
    }.describe {
        operationId = "resetPasswordHTML"
        summary = "[HTML] Password reset endpoint"
        tag("Security")
        parameters {
            q("token", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM", required = true)
            q("newPassword", "hunter2", required = true)
            q("confirmNewPassword", "hunter2", required = true)
        }
        responses {
            successResponse<String>("Success view or an error view, in case the token or password is invalid, or the passwords do not match.")
        }
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
    }.describe {
        operationId = "trustDeviceHTML"
        summary = "[HTML] Trust device endpoint"
        tag("Security")
        parameters {
            q("token", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM", required = true)
        }
        responses {
            successResponse<String>("Success view or an error view, in case the token is invalid.")
        }
    }

    post("/api/mail/verify") {
        val parameters = call.receiveParameters()

        val token = parameters["token"] ?: return@post call.returnHtmlError("web.security.error.missing_token")

        if (SecurityRepo.verifyEmailAddress(token)) {
            return@post call.processDone()
        }
        else {
            return@post call.invalidVerificationToken()
        }
    }.describe {
        operationId = "verifyMailHTML"
        summary = "[HTML] Mail verification endpoint"
        tag("Security")
        parameters {
            q("token", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM", required = true)
        }
        responses {
            successResponse<String>("Success view or an error view, in case the token is invalid.")
        }
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
        }.describe {
            operationId = "getTotpQrCode"
            summary = "Get the QR code for TOTP codes"
            description = "Only available if TOTP is not already configured for the current user."
            tag("Security")
            authResponses(PermissionType.MANAGE_OWN_2FA) {
                successResponse<ByteArray>("PNG image data for the QR code")
                forbiddenResponse("TOTP is already configured for the current user")
            }
        }

        getP("/api/auth/totp", PermissionType.MANAGE_OWN_2FA) {
            val user = call.getUser() ?: unauthorized()

            if (user.totpSecret == null) {
                TotpManager.generateTotpSecret(user)
            }

            call.respond(TotpStatusDto(
                status = user.totpType.name,
                secret = if (user.totpType != TotpType.Totp) user.totpSecret else null
            ))
        }.describe {
            operationId = "getTotpStatus"
            summary = "Get the current TOTP status"
            tag("Security")
            authResponses(PermissionType.MANAGE_OWN_2FA) {
                successResponse<TotpStatusDto>("Current TOTP status")
            }
        }

        putP("/api/auth/totp", PermissionType.MANAGE_OWN_2FA) {
            val user = call.getUser() ?: unauthorized()
            val type = call.getEnum<TotpType>("type", null)

            if (type == TotpType.Mail && !user.emailVerified) {
                conflict("Could not set the TOTP type to Email because the user's email address isn't verified.")
            }

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
        }.describe {
            operationId = "updateTotpType"
            summary = "Update the current TOTP type"
            description = "- When `type` is `Totp`, a 6 digit `code` from the authenticator app must be provided.\n" +
                    "- When `type` is `Mail`, the user's email address must be verified."
            tag("Security")
            parameters {
                q("type", TotpType.Mail, required = true)
                q("code", "123456", required = true,
                    description = "The code from the authenticator app if `type` is `Totp`")
            }
            authResponses(PermissionType.MANAGE_OWN_2FA) {
                successResponse<SuccessDto>("Success")
                invalidParameterResponse("code", "Invalid TOTP code")
                conflictResponse("TOTP type was set to `Email`, but the user's email address isn't verified.")
            }
        }

        postP("/api/auth/mail/verify", PermissionType.MANAGE_OWN_USER) {
            val user = call.getUser() ?: unauthorized()

            val result = MailVerificationManager.sendVerificationMail(user)

            when (result) {
                MailVerificationManager.MailVerificationResult.AlreadyVerified -> conflict("The email address is already verified.")
                MailVerificationManager.MailVerificationResult.InvalidEmail -> conflict("The configured email address is invalid.")
                MailVerificationManager.MailVerificationResult.NotConfigured -> unavailable("SMTP is not configured.")
                MailVerificationManager.MailVerificationResult.RateLimited -> rateLimitExceeded()
                MailVerificationManager.MailVerificationResult.Sent -> call.success()
            }
        }.describe {
            operationId = "sendVerificationMail"
            summary = "Send a verification mail"
            tag("Security")
            authResponses(PermissionType.MANAGE_OWN_USER) {
                successResponse<SuccessDto>("Success")
                conflictResponse("The email address is already verified. / The configured email address is invalid.")
                unavailableResponse("SMTP is not configured.")
                rateLimitedResponse()
            }
        }

        getP("/api/auth/mail/can-verify", PermissionType.MANAGE_OWN_USER) {
            if (!MailSender.configured) {
                return@getP call.success(false)
            }
            val user = call.getUser() ?: unauthorized()
            if (user.email.isNullOrBlank() || user.emailVerified) {
                return@getP call.success(false)
            }
            if (!MailVerificationManager.rateLimiter.check(user.id.value.toString())) {
                return@getP call.success(false)
            }

            return@getP call.success(true)
        }.describe {
            operationId = "canVerifyMail"
            summary = "Check if verification mail can be sent"
            description = "This endpoint checks the following conditions:\n" +
                    "- Is SMTP configured on the server?\n" +
                    "- Does the user have an email address set in their account?\n" +
                    "- Is the user's email address already verified?\n" +
                    "- Is the user currently rate-limited?"
            tag("Security")
            authResponses(PermissionType.MANAGE_OWN_USER) {
                successResponse<SuccessDto>("True if a verification mail can be sent, false if not")
            }
        }
    }

    post("/api/auth/totp") {
        val token = call.getStringParameter("token")
        val code = call.getStringParameter("code")

        val session = SessionRepo.getFromSessionToken(token) ?: unauthorized()
        val user = SessionRepo.getUser(session)

        if (!PermissionRepo.hasPermission(user.id.value, PermissionType.MANAGE_OWN_2FA)) {
            noPermission(PermissionType.MANAGE_OWN_2FA)
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
    }.describe {
        operationId = "verify2fa"
        summary = "Verify a session awaiting 2fa"
        tag("Security")
        parameters {
            q("token", "jQKoZx3QheCLlZGRqR7OmwdjeesiMYHPytH4V5Jz6bkX9kPLqHuIXi45w7Pp6GTM", required = true)
            q("code", "123456", required = true)
        }
        responses {
            missingPermissionsResponse(PermissionType.MANAGE_OWN_2FA)
            rateLimitedResponse()
            unauthorizedResponse("Invalid token")
            invalidParameterResponse("code", "Incorrect code")
            notFoundResponse("Totp type for user")
        }
    }
}