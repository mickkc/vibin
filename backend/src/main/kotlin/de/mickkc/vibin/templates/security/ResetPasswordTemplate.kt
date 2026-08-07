package de.mickkc.vibin.templates.security

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.*
import kotlinx.html.*

class ResetPasswordTemplate(val token: String) : Template<FlowContent> {

    override fun FlowContent.apply() {
        h2 { +TemplateTranslator.getTranslation("web.security.reset_password.title") }
        p("desc") {
            +TemplateTranslator.getTranslation("web.security.reset_password.desc")
        }

        form("/api/mail/reset-password", method = FormMethod.post) {
            input(InputType.hidden, name = "token") {
                value = token
            }

            label {
                htmlFor = "newPasswordInput"
                +TemplateTranslator.getTranslation("web.security.reset_password.new.label")
            }
            input(InputType.password, name = "newPassword") {
                id = "newPasswordInput"
                minLength = "8"
                required = true
            }

            label {
                htmlFor = "repeatNewPasswordInput"
                +TemplateTranslator.getTranslation("web.security.reset_password.repeat.label")
            }
            input(InputType.password, name = "confirmNewPassword") {
                id = "repeatNewPasswordInput"
                minLength = "8"
                required = true
            }

            input(InputType.submit, classes = "btn primary") {
                value = TemplateTranslator.getTranslation("web.security.reset_password.button")
            }
        }
    }
}