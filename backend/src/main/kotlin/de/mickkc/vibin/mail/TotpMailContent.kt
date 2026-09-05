package de.mickkc.vibin.mail

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.*
import kotlinx.html.*

class TotpMailContent(val code: String) : Template<FlowContent> {

    override fun FlowContent.apply() {
        return div {
            style = "margin: 0 0 24px; font-size: 15px; line-height: 1.6; color: #a0b0a0;"
            p {
                +TemplateTranslator.getTranslation("mails.totp.body", 5)
            }

            div {
                style = "padding: 12px 24px; background: #2e332c; text-align: center; border-radius: 12px;"
                p {
                    style = "margin: 0"
                    +TemplateTranslator.getTranslation("mails.totp.code")
                }
                h1 {
                    style = "color: #f8faf0; margin: 0;"
                    +code
                }
            }
        }
    }
}