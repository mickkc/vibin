package de.mickkc.vibin.mail

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.*
import kotlinx.html.*

class NewLoginMailContent(val deviceName: String) : Template<FlowContent> {
    override fun FlowContent.apply() {
        return div {
            style = "margin: 0 0 24px; font-size: 15px; line-height: 1.6; color: #a0b0a0;"
            p {
                +TemplateTranslator.getTranslation("mails.new_sign_in.body")
            }

            ul {
                li {
                    span {
                        style = "font-weight:bold;"
                        +"Device name: "
                    }
                    +deviceName
                }
            }
        }
    }
}