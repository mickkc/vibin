package de.mickkc.vibin.routes

import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.scrobbling.ScrobblingManager
import de.mickkc.vibin.settings.Settings
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureScrobblingRoutes() = routing {
    authenticate("tokenAuth") {

        getP("/api/scrobbling/scrobblers/enabled", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            val scrobblers = ScrobblingManager.getEnabledScrobblers(userId)
            call.respond(scrobblers.map {
                ScrobblingManager.toDto(it, userId)
            })
        }

        getP("/api/scrobbling/scrobblers/all", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            call.respond(ScrobblingManager.scrobblers.map {
                ScrobblingManager.toDto(it, userId)
            })
        }

        putP("/api/scrobbling/scrobblers/{id}/enabled", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            val scrobblerId = call.getStringParameter("id")
            val state = call.getBooleanParameter("state")

            val scrobbler = ScrobblingManager.getScrobblerById(scrobblerId) ?: notFound()

            if (state && !scrobbler.isAvailable(userId))
                forbidden()

            ScrobblingManager.setScrobblerEnabled(userId, scrobblerId, state)

            call.success()
        }

        getP("/api/scrobbling/scrobblers/{id}/settings", PermissionType.MANAGE_SCROBBLING) {
            val userId = call.getUserId() ?: unauthorized()
            val scrobblerId = call.getStringParameter("id")
            val scrobbler = ScrobblingManager.getScrobblerById(scrobblerId) ?: notFound()

            call.respond(scrobbler.settings.associate {
                it.key to Settings.toDto(it, userId)
            })
        }
    }
}