package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.SettingDto
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.scrobbling.ScrobblerDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.scrobbling.ScrobblingManager
import de.mickkc.vibin.settings.Settings
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureScrobblingRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/scrobbling/scrobblers/enabled", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            val scrobblers = ScrobblingManager.getEnabledScrobblers(userId)
            call.respond(scrobblers.map {
                ScrobblingManager.toDto(it, userId)
            })
        }.describe {
            operationId = "getEnabledScrobblers"
            summary = "Get enabled scrobblers"
            tag("Scrobbling")
            authResponses(PermissionType.MANAGE_SCROBBLING) {
                successResponse<List<ScrobblerDto>>("List of enabled scrobblers")
            }
        }

        getP("/api/scrobbling/scrobblers/all", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            call.respond(ScrobblingManager.scrobblers.map {
                ScrobblingManager.toDto(it, userId)
            })
        }.describe {
            operationId = "getAllScrobblers"
            summary = "Get all scrobblers"
            tag("Scrobbling")
            authResponses(PermissionType.MANAGE_SCROBBLING) {
                successResponse<List<ScrobblerDto>>("List of all scrobblers")
            }
        }

        putP("/api/scrobbling/scrobblers/{id}/enabled", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            val scrobblerId = call.getStringParameter("id")
            val state = call.getBooleanParameter("state")

            val scrobbler = ScrobblingManager.getScrobblerById(scrobblerId) ?: notFound()

            if (state && !scrobbler.isAvailable(userId))
                forbidden("Scrobbler is not available")

            ScrobblingManager.setScrobblerEnabled(userId, scrobblerId, state)

            call.success()
        }.describe {
            operationId = "setScrobblerEnabled"
            summary = "Enable or disable a scrobbler"
            tag("Scrobbling")
            parameters {
                p("id", 1L, description = "The scrobbler's Id")
                q("state", true, required = true,
                    description = "true to enable the scrobbler, false to disable")
            }
            authResponses(PermissionType.MANAGE_SCROBBLING) {
                successResponse<SuccessDto>("Success")
                notFoundResponse("Scrobbler")
                forbiddenResponse("Scrobbler is not available")
            }
        }

        getP("/api/scrobbling/scrobblers/{id}/settings", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            val scrobblerId = call.getStringParameter("id")
            val scrobbler = ScrobblingManager.getScrobblerById(scrobblerId) ?: notFound()

            call.respond(scrobbler.settings.associate {
                it.key to Settings.toDto(it, userId)
            })
        }.describe {
            operationId = "getScrobblerSettings"
            summary = "Get a scrobbler's settings"
            tag("Scrobbling")
            parameters {
                p("id", 1L, description = "The scrobbler's Id")
            }
            authResponses(PermissionType.MANAGE_SCROBBLING) {
                successResponse<Map<String, SettingDto<String>>>("The scrobbler's settings")
                notFoundResponse("Scrobbler")
            }
        }
    }
}