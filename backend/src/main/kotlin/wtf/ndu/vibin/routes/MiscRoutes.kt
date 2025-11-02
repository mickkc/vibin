package wtf.ndu.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.WelcomeTexts

fun Application.configureMiscRoutes() = routing {

    get("/api/misc/welcome") {
        val texts = Settings.get(WelcomeTexts)
        if (texts.isNotEmpty()) {
            val randomText = texts.random()
            call.respond(randomText)
        } else {
            call.respond("")
        }
    }

}