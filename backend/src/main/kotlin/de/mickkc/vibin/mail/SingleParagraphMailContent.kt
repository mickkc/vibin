package de.mickkc.vibin.mail

import io.ktor.server.html.*
import kotlinx.html.FlowContent
import kotlinx.html.p
import kotlinx.html.style

class SingleParagraphMailContent(val key: String) : Template<FlowContent> {
    override fun FlowContent.apply() {
        return p {
            style = "margin: 0 0 24px; font-size: 15px; line-height: 1.6; color: #a0b0a0;"
            +MailTranslator.getTranslation(key)
        }
    }
}