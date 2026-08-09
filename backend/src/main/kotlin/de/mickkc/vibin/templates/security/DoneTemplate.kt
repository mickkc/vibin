package de.mickkc.vibin.templates.security

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.Template
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.img
import kotlinx.html.p

class DoneTemplate : Template<FlowContent> {

    override fun FlowContent.apply() {
        div("row") {
            img("Checkmark icon", "/api/web/check_circle.png") {
                width = "24"
                height = "24"
            }
            h2 { +TemplateTranslator.getTranslation("web.security.done.title") }
        }
        p("desc") {
            +TemplateTranslator.getTranslation("web.security.done.desc")
        }
    }
}