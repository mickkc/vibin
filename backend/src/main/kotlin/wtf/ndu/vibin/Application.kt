package wtf.ndu.vibin

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import wtf.ndu.vibin.config.*
import wtf.ndu.vibin.processing.AudioFileProcessor
import wtf.ndu.vibin.routes.*
import wtf.ndu.vibin.tasks.TaskScheduler
import wtf.ndu.vibin.tasks.Tasks

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

    configureFrontendRoutes()

    GlobalScope.launch {
        AudioFileProcessor.initialProcess()
    }

    Tasks.configure()
    TaskScheduler.start()
}
