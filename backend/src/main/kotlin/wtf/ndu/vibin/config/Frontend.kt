package wtf.ndu.vibin.config

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureFrontendRoutes() = routing {

    val frontendDir = EnvUtil.get(EnvUtil.FRONTEND_DIR)

    if (frontendDir != null) {
        val frontendDirFile = File(frontendDir)
        staticFiles("/", frontendDirFile)
    }

}