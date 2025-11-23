package de.mickkc.vibin.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.WelcomeTexts

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