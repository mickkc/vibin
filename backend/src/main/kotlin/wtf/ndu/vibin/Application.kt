package wtf.ndu.vibin

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import wtf.ndu.vibin.config.configureDatabase
import wtf.ndu.vibin.config.configureHTTP
import wtf.ndu.vibin.config.configureOpenApi
import wtf.ndu.vibin.config.configureSecurity
import wtf.ndu.vibin.config.configureSerialization
import wtf.ndu.vibin.config.configureStatusPages
import wtf.ndu.vibin.processing.AudioFileProcessor
import wtf.ndu.vibin.routes.configureAlbumRoutes
import wtf.ndu.vibin.routes.configureArtistRoutes
import wtf.ndu.vibin.routes.configureAuthRoutes
import wtf.ndu.vibin.routes.configureMetadataRoutes
import wtf.ndu.vibin.routes.configurePermissionRoutes
import wtf.ndu.vibin.routes.configurePlaylistRoutes
import wtf.ndu.vibin.routes.configurePlaylistTrackRoutes
import wtf.ndu.vibin.routes.configureStatisticRoutes
import wtf.ndu.vibin.routes.configureTrackRoutes
import wtf.ndu.vibin.routes.configureUserRoutes

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
    configureStatisticRoutes()

    GlobalScope.launch {
        AudioFileProcessor.initialProcess()
    }
}
