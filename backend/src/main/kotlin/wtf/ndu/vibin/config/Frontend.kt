package wtf.ndu.vibin.config

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import wtf.ndu.vibin.routes.forbidden
import kotlin.io.path.Path

fun Application.configureFrontendRoutes() = routing {

    val frontendDir = EnvUtil.get(EnvUtil.FRONTEND_DIR)

    if (frontendDir != null) {
        val frontendDirPath = Path(frontendDir)

        get("/") {
            call.respondRedirect("/web/", permanent = true)
        }

        get(Regex("/web/.*")) {
            val relativePath = call.request.uri.removePrefix("/web/").split("?").first()
            val filePath = frontendDirPath.resolve(relativePath).normalize()

            if (!filePath.startsWith(frontendDirPath)) {
                return@get call.forbidden()
            }

            val file = filePath.toFile()

            if (file.exists() && file.isFile) {
                call.respondFile(file)
            } else {
                call.respondFile(frontendDirPath.resolve("index.html").toFile())
            }
        }
    }

}