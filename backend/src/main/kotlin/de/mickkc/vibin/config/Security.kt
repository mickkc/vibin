package de.mickkc.vibin.config

import io.ktor.server.application.*
import io.ktor.server.auth.*
import de.mickkc.vibin.auth.TokenAuthenticationProvider

fun Application.configureSecurity() {
    authentication {
        register(TokenAuthenticationProvider())
    }
}
