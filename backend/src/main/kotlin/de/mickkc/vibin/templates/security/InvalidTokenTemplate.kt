package de.mickkc.vibin.templates.security

import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.LoginNotificationTokenValidity
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.server.html.*
import kotlinx.html.FlowContent
import kotlinx.html.h2
import kotlinx.html.p

class InvalidTokenTemplate : Template<FlowContent> {

    val tokenValidity = Settings.get(LoginNotificationTokenValidity)

    override fun FlowContent.apply() {
        h2 { +TemplateTranslator.getTranslation("web.security.invalid_token.title") }
        p("desc") {
            +TemplateTranslator.getTranslation("web.security.invalid_token.desc", DateTimeUtils.formatDuration(tokenValidity))
        }
    }
}