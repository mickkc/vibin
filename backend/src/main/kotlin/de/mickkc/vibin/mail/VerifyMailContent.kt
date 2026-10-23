package de.mickkc.vibin.mail

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.Template
import kotlinx.html.*

class VerifyMailContent : Template<FlowContent> {

    override fun FlowContent.apply() {
        return div {
            style = "margin: 0 0 24px; font-size: 15px; line-height: 1.6; color: #a0b0a0;"
            p {
                +TemplateTranslator.getTranslation("mails.verify.body")
            }
        }
    }

}