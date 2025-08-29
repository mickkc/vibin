package wtf.ndu.vibin.config

import io.ktor.server.application.*
import io.ktor.server.auth.*
import wtf.ndu.vibin.auth.TokenAuthenticationProvider

fun Application.configureSecurity() {
    authentication {
        register(TokenAuthenticationProvider())
    }
}
