package de.mickkc.vibin.routes

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.widgets.SharedWidgetEntity
import de.mickkc.vibin.dto.widgets.CreateWidgetDto
import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.repos.WidgetRepo
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.websockets.listener.ListeningToWidgetListener
import de.mickkc.vibin.widgets.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

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

private fun getValidatedWidgetTypes(widget: SharedWidgetEntity): List<WidgetType> {
    val widgetTypes = WidgetRepo.getTypes(widget)

    if (widgetTypes.isEmpty()) {
        invalidParameter("types")
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
        val checksum = call.getStringParameter("checksum")
        val quality = call.getIntOrDefault("quality", 192)

        val expirationTimestamp = call.getLongParameter("exp")
        val signature = call.getStringParameter("sig")

        if (!AccessCryptoUtil.validateSignature(checksum, expirationTimestamp, signature)) {
            forbidden()
        }

        if (checksum.startsWith("default-")) {
            val type = checksum.removePrefix("default-")
            val file = PathUtils.getDefaultImage(type, quality) ?: notFound()
            return@get call.respondFile(file)
        }

        val image = ImageRepo.getBySourceChecksum(checksum) ?: notFound()

        val file = ImageCache.getImageFile(image, quality)
            ?: PathUtils.getThumbnailFileFromPath(image.sourcePath)

        if (!file.exists()) {
            notFound()
        }

        call.respondFile(file)
    }

    getP("/api/widgets/{id}") {
        val id = call.getStringParameter("id")
        val widget = WidgetRepo.getWidget(id) ?: notFound()

        val colors = call.parseWidgetColors(widget)
        val language = call.parseLanguage()
        val widgetTypes = getValidatedWidgetTypes(widget)
        val ctx = createWidgetContext(widget, colors, language)

        call.respondText(WidgetBuilder.build(widgetTypes, ctx), contentType = ContentType.Text.Html)
    }

    getP("/api/widgets/{id}/image") {
        val id = call.getStringParameter("id")
        val widget = WidgetRepo.getWidget(id) ?: notFound()

        val clientId = call.getClientId()
        if (!WidgetImageCache.rateLimiter.checkAndIncrement(clientId)) {
            rateLimitExceeded()
        }

        val width = call.getIntOrDefault("width", 1080).coerceIn(1, 1920)
        // height is optional, if not provided, it gets calculated based on the widget size.
        val height = call.parameters["height"]?.let {
            it.toIntOrNull()?.coerceIn(1, 1920) ?: invalidParameter("height")
        }

        val colors = call.parseWidgetColors(widget)
        val language = call.parseLanguage()
        val widgetTypes = getValidatedWidgetTypes(widget)
        val ctx = createWidgetContext(widget, colors, language)

        val imageBytes = WidgetImageCache.getOrCreateImage(widget, widgetTypes, ctx, width, height)

        val imageCacheDuration = EnvUtil.getOrError(EnvUtil.WIDGET_CACHE_EXPIRATION_MINUTES)
        call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=${imageCacheDuration * 60}, stale-while-revalidate=60")

        call.respondBytes(imageBytes, ContentType.Image.PNG)
    }

    authenticate("tokenAuth") {

        getP("/api/widgets", PermissionType.MANAGE_WIDGETS) {
            val user = call.getUser() ?: unauthorized()

            val widgets = WidgetRepo.getAllForUser(user)
            val widgetDtos = WidgetRepo.toDto(widgets)

            call.respond(widgetDtos)
        }

        postP("/api/widgets", PermissionType.MANAGE_WIDGETS) {

            val user = call.getUser() ?: unauthorized()
            val dto = call.receive<CreateWidgetDto>()

            val parsedTypes = dto.types.map { typeName ->
                try {
                    WidgetType.valueOf(typeName.uppercase())
                } catch (_: IllegalArgumentException) {
                    invalidParameter("types")
                }
            }

            if (parsedTypes.isEmpty()) {
                invalidParameter("types")
            }

            val bgColor = dto.bgColor?.let {
                WidgetUtils.colorFromHex(it) ?: invalidParameter("bgColor")
            }

            val fgColor = dto.fgColor?.let {
                WidgetUtils.colorFromHex(it) ?: invalidParameter("fgColor")
            }

            val accentColor = dto.accentColor?.let {
                WidgetUtils.colorFromHex(it) ?: invalidParameter("accentColor")
            }

            // Check that either all or none of the colors are provided
            val colorList = listOfNotNull(bgColor, fgColor, accentColor)
            if (colorList.size != 3 && colorList.isNotEmpty()) {
                invalidParameter("colors")
            }

            val widget = WidgetRepo.shareWidget(user, parsedTypes, bgColor, fgColor, accentColor)
            call.respond(WidgetRepo.toDto(widget))
        }

        deleteP("/api/widgets/{id}", PermissionType.MANAGE_WIDGETS) {
            val id = call.getStringParameter("id")
            val userId = call.getUserId() ?: unauthorized()

            val widget = WidgetRepo.getWidget(id) ?: notFound()

            val success = WidgetRepo.deleteWidget(widget, userId)
            if (!success) {
                forbidden()
            }

            // Evict cached images for this widget
            WidgetImageCache.evictCacheForWidget(id)

            call.success()
        }
    }

    staticResources("/api/js", "js")

    webSocket("/api/widgets/{id}/listeningto/ws") {
        val id = call.parameters["id"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No Widget ID provided"))
        val widget = WidgetRepo.getWidget(id)
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid widget ID"))

        if (!WidgetRepo.getTypes(widget).contains(WidgetType.LISTENING_TO)) {
            return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Widget does not contain the 'listening to' widget type."))
        }

        val userId = WidgetRepo.getUserId(widget)

        val session = ControlRepo.getCurrentSession(userId)
            ?: ControlRepo.getLatestSession(userId)
            ?: return@webSocket close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "No current or previous session for user."))

        val listener = ListeningToWidgetListener(session, userId, outgoing)

        try {
            listener.reconnect()
            incoming.consumeEach { frame ->
                outgoing.send(frame)
            }
        }
        catch (e: Exception) {
            listener.logger.error("Error in websocket connection to listening status.", e)
        }
        finally {
            listener.stop()
        }
    }
}