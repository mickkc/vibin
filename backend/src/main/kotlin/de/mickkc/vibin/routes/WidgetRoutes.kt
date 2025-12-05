package de.mickkc.vibin.routes

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.widgets.SharedWidgetEntity
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

private data class WidgetColors(
    val backgroundColor: Int,
    val foregroundColor: Int,
    val accentColor: Int
)

private fun RoutingCall.parseWidgetColors(widget: SharedWidgetEntity): WidgetColors {
    val backgroundColor = request.queryParameters["bgColor"]?.let {
        WidgetUtils.colorFromHex(it)
    } ?: widget.bgColor ?: 0x1D2021

    val foregroundColor = request.queryParameters["fgColor"]?.let {
        WidgetUtils.colorFromHex(it)
    } ?: widget.fgColor ?: 0xEBDBB2

    val accentColor = request.queryParameters["accentColor"]?.let {
        WidgetUtils.colorFromHex(it)
    } ?: widget.accentColor ?: 0x689D6A

    return WidgetColors(backgroundColor, foregroundColor, accentColor)
}

private fun RoutingCall.parseLanguage(): String {
    var language = request.queryParameters["lang"]
        ?: request.header("Accept-Language")?.split(",", "-")?.firstOrNull()?.lowercase() ?: "en"

    if (!WidgetBuilder.SUPPORTED_LANGUAGES.contains(language)) {
        language = "en"
    }

    return language
}

private suspend fun RoutingCall.getValidatedWidgetTypes(widget: SharedWidgetEntity): List<WidgetType>? {
    val widgetTypes = WidgetRepo.getTypes(widget)

    if (widgetTypes.isEmpty()) {
        invalidParameter("types")
        return null
    }

    return widgetTypes
}

private fun createWidgetContext(
    widget: SharedWidgetEntity,
    colors: WidgetColors,
    language: String
): WidgetContext {
    return WidgetContext(
        userId = WidgetRepo.getUserId(widget),
        accentColor = colors.accentColor,
        backgroundColor = colors.backgroundColor,
        foregroundColor = colors.foregroundColor,
        language = language
    )
}

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
        val checksum = call.getStringParameter("checksum") ?: return@get
        val quality = call.getIntOrDefault("quality", 192) ?: return@get

        val expirationTimestamp = call.getLongParameter("exp") ?: return@get
        val signature = call.getStringParameter("sig") ?: return@get

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
        val id = call.getStringParameter("id") ?: return@getP
        val widget = WidgetRepo.getWidget(id) ?: return@getP call.notFound()

        val colors = call.parseWidgetColors(widget)
        val language = call.parseLanguage()
        val widgetTypes = call.getValidatedWidgetTypes(widget) ?: return@getP
        val ctx = createWidgetContext(widget, colors, language)

        call.respondText(WidgetBuilder.build(widgetTypes, ctx), contentType = ContentType.Text.Html)
    }

    getP("/api/widgets/{id}/image") {
        val id = call.getStringParameter("id") ?: return@getP
        val widget = WidgetRepo.getWidget(id) ?: return@getP call.notFound()

        val clientId = call.getClientId()
        if (!WidgetImageCache.rateLimiter.checkAndIncrement(clientId)) {
            return@getP call.rateLimitExceeded()
        }

        val width = call.getIntOrDefault("width", 1080)?.coerceIn(1, 1920) ?: return@getP
        val height = call.getIntOrDefault("height", 720)?.coerceIn(1, 1920) ?: return@getP

        val colors = call.parseWidgetColors(widget)
        val language = call.parseLanguage()
        val widgetTypes = call.getValidatedWidgetTypes(widget) ?: return@getP
        val ctx = createWidgetContext(widget, colors, language)

        val imageBytes = WidgetImageCache.getOrCreateImage(widget, widgetTypes, ctx, width, height)

        val imageCacheDuration = EnvUtil.getOrDefault(EnvUtil.WIDGET_CACHE_EXPIRATION_MINUTES, EnvUtil.DEFAULT_WIDGET_CACHE_EXPIRATION_MINUTES).toLong()
        call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=${imageCacheDuration * 60}, stale-while-revalidate=60")

        call.respondBytes(imageBytes, ContentType.Image.PNG)
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
            val id = call.getStringParameter("id") ?: return@deleteP
            val userId = call.getUserId() ?: return@deleteP call.unauthorized()

            val widget = WidgetRepo.getWidget(id) ?: return@deleteP call.notFound()

            val success = WidgetRepo.deleteWidget(widget, userId)
            if (!success) {
                return@deleteP call.forbidden()
            }

            // Evict cached images for this widget
            WidgetImageCache.evictCacheForWidget(id)

            call.success()
        }
    }
}