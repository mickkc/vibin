package de.mickkc.vibin.routes

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureCheckRoutes() = routing {

    get("/api/check") {
        call.respond(mapOf(
            "status" to "ok",
            "version" to de.mickkc.vibin.version
        ))
    }

}