package de.mickkc.vibin.templates.security

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.Template
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.form
import kotlinx.html.h2
import kotlinx.html.input
import kotlinx.html.p

class VerifyEmailAddressTemplate(val token: String, val emailAddress: String) : Template<FlowContent> {

    override fun FlowContent.apply() {
        h2 { +TemplateTranslator.getTranslation("web.security.verify_email.title") }
        p("desc") {
            +TemplateTranslator.getTranslation("web.security.verify_email.desc", emailAddress)
        }

        form("/api/mail/verify", method = FormMethod.post) {
            input(InputType.hidden, name = "token") {
                value = token
            }

            input(InputType.submit, classes = "btn primary") {
                value = TemplateTranslator.getTranslation("web.security.verify_email.verify")
            }
        }
    }
}