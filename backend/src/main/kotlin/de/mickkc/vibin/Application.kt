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
    val host = EnvUtil.getOrError(EnvUtil.SERVER_HOST)
    val port = EnvUtil.getOrError(EnvUtil.SERVER_PORT)

    embeddedServer(Netty, port = port, host = host, module = Application::mainModule)
        .start(wait = true)
}

val version: String
    get() = "0.0.1-beta.14"

val supportedAppVersions: List<String>
    get() = listOf("0.0.1-beta.12", "0.0.1-beta.13", "0.0.1-beta.14")

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
    configureControlRoutes()

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
