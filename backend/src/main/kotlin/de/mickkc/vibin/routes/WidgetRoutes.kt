package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.widgets.CreateWidgetDto
import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.repos.WidgetRepo
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.widgets.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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

    getP("/api/widgets/{id}") {
        val id = call.parameters["id"] ?: return@getP call.missingParameter("id")

        val widget = WidgetRepo.getWidget(id) ?: return@getP call.notFound()

        val backgroundColor = call.request.queryParameters["bgColor"]?.let {
            WidgetUtils.colorFromHex(it)
        } ?: widget.bgColor ?: 0x1D2021

        val foregroundColor = call.request.queryParameters["fgColor"]?.let {
            WidgetUtils.colorFromHex(it)
        } ?: widget.fgColor ?: 0xEBDBB2

        val accentColor = call.request.queryParameters["accentColor"]?.let {
            WidgetUtils.colorFromHex(it)
        } ?: widget.accentColor ?: 0x689D6A

        var language = call.request.queryParameters["lang"]
            ?: call.request.header("Accept-Language")?.split(",", "-")?.firstOrNull()?.lowercase() ?: "en"

        if (!WidgetBuilder.SUPPORTED_LANGUAGES.contains(language)) {
            language = "en"
        }

        val ctx = WidgetContext(
            userId = WidgetRepo.getUserId(widget),
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            foregroundColor = foregroundColor,
            language = language
        )

        val widgetTypes = WidgetRepo.getTypes(widget)

        if (widgetTypes.isEmpty()) {
            return@getP call.invalidParameter("types")
        }

        call.respondText(WidgetBuilder.build(widgetTypes, ctx), contentType = ContentType.Text.Html)
    }

    authenticate("tokenAuth") {

        getP("/api/widgets", PermissionType.MANAGE_WIDGETS) {
            val user = call.getUser() ?: return@getP call.unauthorized()

            val widgets = WidgetRepo.getAllForUser(user)
            val widgetDtos = WidgetRepo.toDto(widgets)

            call.respond(widgetDtos)
        }

        postP("/api/widgets", PermissionType.MANAGE_WIDGETS) {

            val user = call.getUser() ?: return@postP call.unauthorized()
            val dto = call.receive<CreateWidgetDto>()

            val parsedTypes = dto.types.map { typeName ->
                try {
                    WidgetType.valueOf(typeName.uppercase())
                } catch (_: IllegalArgumentException) {
                    return@postP call.invalidParameter("types")
                }
            }

            if (parsedTypes.isEmpty()) {
                return@postP call.invalidParameter("types")
            }

            val bgColor = dto.bgColor?.let {
                WidgetUtils.colorFromHex(it) ?: return@postP call.invalidParameter("bgColor")
            }

            val fgColor = dto.fgColor?.let {
                WidgetUtils.colorFromHex(it) ?: return@postP call.invalidParameter("fgColor")
            }

            val accentColor = dto.accentColor?.let {
                WidgetUtils.colorFromHex(it) ?: return@postP call.invalidParameter("accentColor")
            }

            // Check that either all or none of the colors are provided
            val colorList = listOfNotNull(bgColor, fgColor, accentColor)
            if (colorList.size != 3 && colorList.isNotEmpty()) {
                return@postP call.invalidParameter("colors")
            }

            val widget = WidgetRepo.shareWidget(user, parsedTypes, bgColor, fgColor, accentColor)
            call.respond(WidgetRepo.toDto(widget))
        }

        deleteP("/api/widgets/{id}", PermissionType.MANAGE_WIDGETS) {
            val id = call.parameters["id"] ?: return@deleteP call.missingParameter("id")
            val userId = call.getUserId() ?: return@deleteP call.unauthorized()

            val widget = WidgetRepo.getWidget(id) ?: return@deleteP call.notFound()

            val success = WidgetRepo.deleteWidget(widget, userId)
            if (!success) {
                return@deleteP call.forbidden()
            }

            call.success()
        }
    }
}