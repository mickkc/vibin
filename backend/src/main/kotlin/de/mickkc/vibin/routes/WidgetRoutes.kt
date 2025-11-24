package de.mickkc.vibin.routes

import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.widgets.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureWidgetRoutes() = routing {

    get("/api/widgets/styles") {
        call.respondText(WidgetStyles.render(), contentType = ContentType.Text.CSS)
    }

    /**
     * Endpoint to serve images for widgets with signature-based authentication.
     * This ensures only images that have been explicitly signed can be accessed.
     * This avoids brute-forcing checksums and accessing images that where valid at some point.
     */
    get("/api/widgets/images/{checksum}") {
        val checksum = call.parameters["checksum"] ?: return@get call.missingParameter("checksum")
        val quality = call.request.queryParameters["quality"]?.toIntOrNull() ?: 192

        val expirationTimestamp = call.request.queryParameters["exp"]?.toLongOrNull() ?: return@get call.missingParameter("exp")
        val signature = call.request.queryParameters["sig"] ?: return@get call.missingParameter("sig")

        if (!ImageCryptoUtil.validateImageSignature(checksum, expirationTimestamp, signature)) {
            return@get call.forbidden()
        }

        if (checksum.startsWith("default-")) {
            val type = checksum.removePrefix("default-")
            val file = PathUtils.getDefaultImage(type, quality) ?: return@get call.notFound()
            return@get call.respondFile(file)
        }

        val image = ImageRepo.getBySourceChecksum(checksum) ?: return@get call.notFound()

        val file = ImageCache.getImageFile(image, quality)
            ?: PathUtils.getThumbnailFileFromPath(image.sourcePath)

        if (!file.exists()) {
            return@get call.notFound()
        }

        call.respondFile(file)
    }

    getP("/api/widgets") {

        val userId = call.parameters["userId"]?.toLongOrNull()
            ?: return@getP call.missingParameter("userId")

        val backgroundColor = call.request.queryParameters["bgColor"]?.toIntOrNull() ?: 0x1D2021
        val foregroundColor = call.request.queryParameters["fgColor"]?.toIntOrNull() ?: 0xEBDBB2
        val accentColor = call.request.queryParameters["accentColor"]?.toIntOrNull() ?: 0x689D6A
        var language = call.request.queryParameters["lang"]
            ?: call.request.header("Accept-Language")?.split(",", "-")?.firstOrNull()?.lowercase() ?: "en"

        if (!WidgetBuilder.SUPPORTED_LANGUAGES.contains(language)) {
            language = "en"
        }

        val ctx = WidgetContext(
            userId = userId,
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            language = language
        )

        val widgetTypeParam = call.request.queryParameters["types"] ?: "0,1"

        val widgetTypes = widgetTypeParam.split(",").mapNotNull {
            it.toIntOrNull()?.let { WidgetType.entries.getOrNull(it) }
        }.distinct()

        if (widgetTypes.isEmpty()) {
            return@getP call.invalidParameter("types")
        }

        call.respondText(WidgetBuilder.build(widgetTypes, ctx), contentType = ContentType.Text.Html)
    }
}