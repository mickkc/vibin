package de.mickkc.vibin.mail

import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.TotpMailExpirationDuration
import de.mickkc.vibin.templates.TemplateTranslator
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.server.html.*
import kotlinx.html.*

class TotpMailContent(val code: String) : Template<FlowContent> {

    val expirationDuration = DateTimeUtils.formatDuration(Settings.get(TotpMailExpirationDuration))

    override fun FlowContent.apply() {
        return div {
            style = "margin: 0 0 24px; font-size: 15px; line-height: 1.6; color: #a0b0a0;"
            p {
                +TemplateTranslator.getTranslation("mails.totp.body", expirationDuration)
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