package de.mickkc.vibin.config

import de.mickkc.vibin.routes.forbidden
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureFrontendRoutes() = routing {

    val frontendPath = EnvUtil.getOrNull(EnvUtil.FRONTEND_DIR)?.toPath()

    if (frontendPath != null) {

        get("/") {
            call.respondRedirect("/web/", permanent = true)
        }

        get(Regex("/web/.*")) {
            val relativePath = call.request.uri.removePrefix("/web/").split("?").first()
            val filePath = frontendPath.resolve(relativePath).normalize()

            if (!filePath.startsWith(frontendPath)) {
                return@get call.forbidden()
            }

            val file = filePath.toFile()

            if (file.exists() && file.isFile) {
                call.respondFile(file)
            } else {
                call.respondFile(frontendPath.resolve("index.html").toFile())
            }
        }
    }

}