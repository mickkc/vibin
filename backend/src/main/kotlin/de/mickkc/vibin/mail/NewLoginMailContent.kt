package de.mickkc.vibin.mail

import de.mickkc.vibin.security.IpLookupManager
import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.*
import kotlinx.html.*

class NewLoginMailContent(val deviceName: String, val ip: String?, val ipLocation: IpLookupManager.IpLocationInfo?) : Template<FlowContent> {

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
                        +TemplateTranslator.getTranslation("mails.new_sign_in.device_name")
                    }
                    +deviceName
                }
                if (ip != null) {
                    li {
                        span {
                            style = "font-weight:bold;"
                            +TemplateTranslator.getTranslation("mails.new_sign_in.ip_address")
                        }
                        +ip
                    }
                }
                if (ipLocation != null) {
                    li {
                        span {
                            style = "font-weight:bold;"
                            +TemplateTranslator.getTranslation("mails.new_sign_in.location")
                        }
                        +listOfNotNull(ipLocation.city, ipLocation.region_name, ipLocation.country)
                            .joinToString()
                            .ifBlank { TemplateTranslator.getTranslation("mails.new_sign_in.location.error") }
                    }
                }
            }
        }
    }
}