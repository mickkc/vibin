package de.mickkc.vibin.config

import de.mickkc.vibin.utils.BuildInfo
import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*

fun Application.configureOpenApi() = routing {

    staticResources("/api/swagger/static", "openapi.static")

    swaggerUI("/api/docs") {
        info = OpenApiInfo("Vibin'", BuildInfo.APP_VERSION)
        source = OpenApiDocSource.Routing(
            contentType = ContentType.Application.Json
        )
        registerApiKeySecurityScheme(
            name = "tokenAuth",
            keyName = "Authorization",
            keyLocation = SecuritySchemeIn.HEADER,
            description = "Bearer session token"
        )
        // TODO: Fix styles
        //customStyle("/api/swagger/static/swagger-dark.css")
    }
}