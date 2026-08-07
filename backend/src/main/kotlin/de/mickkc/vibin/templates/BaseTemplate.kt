package de.mickkc.vibin.templates

import io.ktor.server.html.*
import kotlinx.html.*

class BaseTemplate(val pageTitle: String) : Template<HTML> {

    val content = Placeholder<FlowContent>()

    override fun HTML.apply() {
        head {
            meta(charset = "UTF-8")
            meta {
                httpEquiv = "X-UA-Compatible"
                content = "ie=edge"
            }
            meta("viewport", "width=device-width, initial-scale=1")
            title { +"Vibin' - $pageTitle" }
            link(rel = "stylesheet", href = "/api/web/styles.css")
            link(rel = "icon", href = "/api/web/favicon.ico")
        }
        body {
            header {
                img("Vibin' Logo", "/api/web/library_music.png") {
                    width = "24"
                    height = "24"
                }
                h1 {
                    +"Vibin'"
                }
            }
            main {
                insert(content)
            }
        }
    }
}