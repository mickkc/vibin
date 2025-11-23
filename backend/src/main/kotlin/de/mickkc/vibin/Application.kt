package de.mickkc.vibin

import de.mickkc.vibin.config.*
import de.mickkc.vibin.processing.AudioFileProcessor
import de.mickkc.vibin.routes.*
import de.mickkc.vibin.tasks.Tasks
import de.mickkc.vibin.tasks.TaskScheduler
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::mainModule)
        .start(wait = true)
}

val version: String
    get() = "0.0.1"

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

    GlobalScope.launch {
        AudioFileProcessor.initialProcess()
    }

    Tasks.configure()
    TaskScheduler.start()
}
