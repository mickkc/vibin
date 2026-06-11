package de.mickkc.vibin

import de.mickkc.vibin.config.*
import de.mickkc.vibin.mail.MailSender
import de.mickkc.vibin.routes.*
import de.mickkc.vibin.tasks.TaskScheduler
import de.mickkc.vibin.tasks.Tasks
import de.mickkc.vibin.watcher.DirectoryWatcher
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::mainModule)
        .start(wait = true)
}

val version: String
    get() = "0.0.1-beta.11"

val supportedAppVersions: List<String>
    get() = listOf("0.0.1-beta.10", "0.0.1-beta.11")

fun Application.mainModule() {
    configureDatabase()
    module()
}

fun Application.module() {

    configureCheckRoutes()

    MailSender.init()

    configureSerialization()
    configureSecurity()
    configureHTTP()
    configureStatusPages()
    configureWebSockets()

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

    configureWebSocketRoutes()

    configureStreamRoutes()
    configureImageRoutes()

    Tasks.configure()
    TaskScheduler.start()

    DirectoryWatcher.configure()
    launch {
        DirectoryWatcher.start()
    }
}
