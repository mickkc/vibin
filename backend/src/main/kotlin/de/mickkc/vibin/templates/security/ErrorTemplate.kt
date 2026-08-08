package de.mickkc.vibin.templates.security

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.EXTERNAL_URL
import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.http.Url
import io.ktor.server.html.Template
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.img
import kotlinx.html.p

class ErrorTemplate(val errorKey: String, refererHeader: String?) : Template<FlowContent> {

    val validatedRefererUrl = refererHeader?.takeIf { referer ->
        val external = Url(EnvUtil.getOrNull(EXTERNAL_URL) ?: return@takeIf false)
        val parsed = Url(referer)
        parsed.protocol == external.protocol &&
                parsed.host.equals(external.host, ignoreCase = true) &&
                parsed.port == external.port
    }

    override fun FlowContent.apply() {
        div("row") {
            img("Error icon", "/api/web/error.png") {
                width = "24"
                height = "24"
            }
            h2 { +TemplateTranslator.getTranslation("web.security.error.title") }
        }
        p("desc") {
            +TemplateTranslator.getTranslation(errorKey)
        }
        if (validatedRefererUrl != null) {
            a(validatedRefererUrl, classes = "btn primary") {
                +TemplateTranslator.getTranslation("web.security.error.go_back")
            }
        }
    }
}