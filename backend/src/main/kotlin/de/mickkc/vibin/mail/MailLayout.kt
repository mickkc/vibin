package de.mickkc.vibin.mail

import io.ktor.server.html.*
import kotlinx.html.*

class MailLayout(val subjectKey: String) : Template<HTML> {

    data class MailLayoutButton(
        val titleKey: String,
        val url: String
    )

    /**
     * Language of the e-mail (en, de, ...)
     */
    var language: String? = MailTranslator.language

    /**
     * Big header text above the content.
     * If the text is null, no header will be displayed.
     */
    var headerTextKey: String? = null

    /**
     * The actual content.
     */
    val content = Placeholder<FlowContent>()

    /**
     * The primary (filled) button, where the first value is the text,
     * and the second value is the url that opens when the user clicks on the button.
     */
    var primaryButton: MailLayoutButton? = null

    /**
     * The secondary (outlined) button, where the first value is the text,
     * and the second value is the url that opens when the user clicks on the button.
     */
    var secondaryButton: MailLayoutButton? = null

    private fun buildStyle(): String {
        return """
            body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
            table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
            img { border: 0; height: auto; line-height: 100%; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic; }
            body { margin: 0 !important; padding: 0 !important; width: 100% !important; }
            a { color: #72c472; text-decoration: none; }
            a:hover { text-decoration: underline; }
        """.trimIndent()
    }

    fun TABLE.applyTableStyle() {
        role = "presentation"
        attributes["cellPadding"] = "0"
        attributes["cellspacing"] = "0"
        attributes["border"] = "0"
    }

    override fun HTML.apply() {
        if (language != null) {
            this.lang = language!!
        }

        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
            meta(name = "color-scheme", content = "dark")
            meta(name = "supported-color-schemes", content = "dark")
            title(content = MailTranslator.getTranslation(subjectKey))

            style {
                unsafe {
                    +buildStyle()
                }
            }
        }
        body {
            attributes["bgcolor"] = "#10140F"
            style = "margin: 0; padding: 0; background-color: #10140F; font-family: Roboto, 'Google Sans', 'Segoe UI', Helvetica, Arial, sans-serif;"

            table {
                applyTableStyle()
                attributes["width"] = "100%"
                attributes["bgcolor"] = "#10140F"
                style = "background-color: #10140F;"

                tr {
                    td {
                        attributes["bgcolor"] = "#1D211B"
                        style = "background-color: #1D211B; border-radius: 16px 16px 0 0; padding: 28px 32px; border-bottom: 1px solid #2e332c;"

                        table {
                            applyTableStyle()
                            attributes["width"] = "100%"

                            tr {
                                td {
                                    attributes["width"] = "24"
                                    img {
                                        src = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAB7klEQVRIS7VVzU7CQBDubhUEbh6rKIlBDxwVD8oT+AC+sF5MiHoyShNLKcGD8eBBQJK2zm6Z3dn+0KCyCaGd3dnvZ2a3zNrwYBve3yoECILX89AO76wYKIifGGJ1LF6SNPlK/hVZbp22nPY9rskVMRy7uG3uPIdoRFHgWcXgubXfliwKFWQBiIylEs6YFRXQWBNAcBX89Lj6XFSfOp2FiLiuW92uWXM6nwHw/ME3s1kEEzWx0AeLMuSIB7gBBcCaiFgGAC3BCWWRyEIjpenJEOu0jVmFpQAjUCD3Q9aUXhqAw8oYFirJMRA4Nou8UgFJRDmmgkTVyi5KA+TV4AsK24HCTvqTunPmTM1OM20qtSjdpod7D1uMXYe0U1adlRyAFzCCq+rLZOI77RoEWUvBaDyAojITgNCtdBsN582ZYigIgt2QzT+Mw0Fe1rZI5Nrxe73ZvJh5nrfDKouZxaCs2LqgloEH2A+lAKpNiyiWxAsAIrDoRPaveYggxOEHbGkrJhi6ezichyhKrpRSBb7/2Ivtxk2GKCl83o0qzwPn3QPnqC+e1W06nEDXAJm8bvmlSzJNAyzvfzxMf9mU5mYAtMf6y6W/W6n7QO80x1s4TUwB+P7zZWzzWyHKLCQFSqWL2sM0OFsO8F+WFCrYFMAPtqEGKK0QXX0AAAAQZGVCR0Y0OTk4RjZDQjQ5N0FCODH40mByAAAAAElFTkSuQmCC"
                                        alt = "Vibin' Logo"
                                        width = "24"
                                        height = "24"
                                    }
                                }

                                td {
                                    span {
                                        style = "font-size: 20px; font-weight: 600; color: #A1D39A; letter-spacing: 0.5px; margin-left: 12px;"
                                        +"Vibin'"
                                    }
                                }
                            }
                        }
                    }
                }

                tr {
                    td {
                        attributes["bgcolor"] = "#1D211B"
                        style = "background-color: #1D211B; padding: 40px 32px 32px; border-radius: 0 0 16px 16px;"
                        if (headerTextKey != null) {
                            h1 {
                                style = "margin: 0 0 16px; font-size: 28px; font-weight: 600; line-height: 1.25; color: #E0E4DB; letter-spacing: -0.3px;"
                                +MailTranslator.getTranslation(headerTextKey!!)
                            }
                        }
                        insert(content)
                        table {
                            applyTableStyle()

                            tr {
                                if (primaryButton != null) {
                                    td {
                                        attributes["bgcolor"] = "#A1D39A"
                                        style = "background-color: #A1D39A; border-radius: 100px; padding: 0;"

                                        a(primaryButton!!.url) {
                                            style = "display: inline-block; padding: 12px 28px; font-size: 14px; font-weight: 600; color: #10140F; text-decoration: none; letter-spacing: 0.25px;"
                                            +MailTranslator.getTranslation(primaryButton!!.titleKey)
                                        }
                                    }
                                }

                                if (secondaryButton != null) {
                                    td {
                                        style = "padding-left: 12px;"

                                        a(secondaryButton!!.url) {
                                            style = "display: inline-block; padding: 11px 28px; font-size: 14px; font-weight: 600; color: #A1D39A; text-decoration: none; letter-spacing: 0.25px; border: 1.5px solid #A1D39A; border-radius: 100px;"
                                            +MailTranslator.getTranslation(secondaryButton!!.titleKey)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}