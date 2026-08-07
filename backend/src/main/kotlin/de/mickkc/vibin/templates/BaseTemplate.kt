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
                img("Vibin' Logo", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAB7klEQVRIS7VVzU7CQBDubhUEbh6rKIlBDxwVD8oT+AC+sF5MiHoyShNLKcGD8eBBQJK2zm6Z3dn+0KCyCaGd3dnvZ2a3zNrwYBve3yoECILX89AO76wYKIifGGJ1LF6SNPlK/hVZbp22nPY9rskVMRy7uG3uPIdoRFHgWcXgubXfliwKFWQBiIylEs6YFRXQWBNAcBX89Lj6XFSfOp2FiLiuW92uWXM6nwHw/ME3s1kEEzWx0AeLMuSIB7gBBcCaiFgGAC3BCWWRyEIjpenJEOu0jVmFpQAjUCD3Q9aUXhqAw8oYFirJMRA4Nou8UgFJRDmmgkTVyi5KA+TV4AsK24HCTvqTunPmTM1OM20qtSjdpod7D1uMXYe0U1adlRyAFzCCq+rLZOI77RoEWUvBaDyAojITgNCtdBsN582ZYigIgt2QzT+Mw0Fe1rZI5Nrxe73ZvJh5nrfDKouZxaCs2LqgloEH2A+lAKpNiyiWxAsAIrDoRPaveYggxOEHbGkrJhi6ezichyhKrpRSBb7/2Ivtxk2GKCl83o0qzwPn3QPnqC+e1W06nEDXAJm8bvmlSzJNAyzvfzxMf9mU5mYAtMf6y6W/W6n7QO80x1s4TUwB+P7zZWzzWyHKLCQFSqWL2sM0OFsO8F+WFCrYFMAPtqEGKK0QXX0AAAAQZGVCR0Y0OTk4RjZDQjQ5N0FCODH40mByAAAAAElFTkSuQmCC") {
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