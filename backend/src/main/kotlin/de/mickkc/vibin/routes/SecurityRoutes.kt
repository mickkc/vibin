package de.mickkc.vibin.routes

import de.mickkc.vibin.security.LoginNotificationManager
import de.mickkc.vibin.templates.BaseTemplate
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.templates.security.InvalidTokenTemplate
import de.mickkc.vibin.templates.security.ResetPasswordTemplate
import de.mickkc.vibin.templates.security.TrustDeviceTemplate
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureSecurityRoutes() = routing {

    staticResources("/api/web", "web.static")

    get("/api/mail/reset-password") {
        val token = call.getStringParameter("tk") ?: return@get

        if (!LoginNotificationManager.isLoginNotificationTokenValid(token)) {
            return@get call.respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.invalid_token.title"))) {
                content {
                    insert(InvalidTokenTemplate()) {}
                }
            }
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
            return@get call.respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.invalid_token.title"))) {
                content {
                    insert(InvalidTokenTemplate()) {}
                }
            }
        }

        call.respondHtmlTemplate(BaseTemplate(TemplateTranslator.getTranslation("web.security.reset_password.title"))) {
            content {
                insert(TrustDeviceTemplate(token)) {}
            }
        }
    }
}