package de.mickkc.vibin.config

import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleMemoryCache.memoryCache
import de.mickkc.vibin.supportedAppVersions
import de.mickkc.vibin.utils.BuildInfo
import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.partialcontent.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureHTTP() {

    install(SimpleCache) {
        memoryCache {
            invalidateAt = 10.seconds
        }
    }
    install(PartialContent)
    install(DefaultHeaders) {
        header("X-Server-Version", BuildInfo.APP_VERSION)
        header("X-Supported-App-Versions", supportedAppVersions.joinToString(","))
    }
}
