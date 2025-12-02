package de.mickkc.vibin

import de.mickkc.vibin.config.*
import de.mickkc.vibin.routes.*
import de.mickkc.vibin.tasks.TaskScheduler
import de.mickkc.vibin.tasks.Tasks
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::mainModule)
        .start(wait = true)
}

val version: String
    get() = "0.0.1-beta.3"

val supportedAppVersions: List<String>
    get() = listOf("0.0.1-beta.3")

fun Application.mainModule() {
    configureDatabase()
    module()
}

fun Application.module() {

    configureCheckRoutes()

    configureSerialization()
    configureSecurity()
    configureHTTP()
    configureStatusPages()

    configureOpenApi()

    configureMiscRoutes()

    configureAuthRoutes()
    configureUserRoutes()

    configureMetadataRoutes()

    configureTrackRoutes()
    configureRelatedTrackRoutes()
    configureAlbumRoutes()
    configurePlaylistRoutes()
    configurePlaylistTrackRoutes()
    configurePermissionRoutes()
    configureArtistRoutes()
    configureTagRoutes()
    configureStatisticRoutes()

    configureTaskRoutes()
    configureUploadRoutes()
    configureSettingRoutes()

    configureFavoriteRoutes()

    configureFrontendRoutes()
    configureWidgetRoutes()

    Tasks.configure()
    TaskScheduler.start()
}
