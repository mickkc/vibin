package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.HealthCheckDto
import de.mickkc.vibin.supportedAppVersions
import de.mickkc.vibin.utils.BuildInfo
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureCheckRoutes() = routing {

    get("/api/check") {
        call.respond(HealthCheckDto(
            status = "ok",
            version = BuildInfo.APP_VERSION,
            supportedAppVersions = supportedAppVersions
        ))
    }.describe {
        operationId = "healthCheck"
        summary = "Health-check endpoint"
        tag("Check")
        responses {
            successResponse<HealthCheckDto>("Service is reachable.")
        }
    }

}