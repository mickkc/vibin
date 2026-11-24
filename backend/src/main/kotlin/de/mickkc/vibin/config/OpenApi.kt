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
        remotePath = "documentation.json"
        info = OpenApiInfo(
            title = "Vibin' API",
            description = """
                API for the Vibin' music server.
                
                All parameters marked as required must be provided when making a request. If not, an error similar to
                this will be returned:
                
                ```json
                {
                  "type": "MISSING_PARAMETER",
                  "data": {
                    "parameterName": "username"
                  }
                }
                ```
                
                Also, when a parameter is in the wrong format, e.g. a string instead of an integer, an error similar to
                this will be returned:
                
                ```json
                {
                  "type": "INVALID_PARAMETER",
                  "data": {
                    "parameterName": "albumId",
                    "reason": "..."
                  }
                }
                ```
                
                To keep it simple, these responses are not added to every single endpoint that expects parameters.
                
                Also check out the [Websocket documentation](https://vibin.rip/docs/websockets).
            """.trimIndent(),
            version = BuildInfo.APP_VERSION,
            contact = OpenApiInfo.Contact(
                name = "Mickkc",
                url = "https://matrix.to/#/@mickkc:matrix.org",
                email = "contact@mickkc.dev"
            ),
            license = OpenApiInfo.License(
                name = "Licensed under GPL v3",
                url = "https://codeberg.org/mickkc/vibin/src/branch/main/LICENSE.md",
                identifier = "GPLv3"
            )
        )
        source = OpenApiDocSource.Routing(
            contentType = ContentType.Application.Json
        )
        registerApiKeySecurityScheme(
            name = "tokenAuth",
            keyName = "Authorization",
            keyLocation = SecuritySchemeIn.HEADER,
            description = "Bearer session token"
        )
        servers {
            server("/") { description = "The server you are currently on." }
            server("https://demo.vibin.mickkc.dev") { description = "A demo instance. Username: Demo, Password: DemoDemo" }
            server("http://localhost:8080")
        }
        // TODO: Fix styles
        //customStyle("/api/web/swagger-dark.css")
    }
}