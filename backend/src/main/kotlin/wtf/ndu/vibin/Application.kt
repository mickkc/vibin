package wtf.ndu.vibin

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import wtf.ndu.vibin.config.configureDatabase
import wtf.ndu.vibin.config.configureHTTP
import wtf.ndu.vibin.config.configureSecurity
import wtf.ndu.vibin.config.configureSerialization
import wtf.ndu.vibin.processing.AudioFileProcessor
import wtf.ndu.vibin.routes.configureAlbumRoutes
import wtf.ndu.vibin.routes.configureAuthRoutes
import wtf.ndu.vibin.routes.configureMetadataRoutes
import wtf.ndu.vibin.routes.configurePlaylistRoutes
import wtf.ndu.vibin.routes.configureTrackRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

val version: String
    get() = "0.0.1"

fun Application.module() {

    configureDatabase()
    configureSerialization()
    configureSecurity()
    configureHTTP()

    configureAuthRoutes()
    configureMetadataRoutes()

    configureTrackRoutes()
    configureAlbumRoutes()
    configurePlaylistRoutes()
    configurePermissionRoutes()

    GlobalScope.launch {
        AudioFileProcessor.initialProcess()
    }
}
