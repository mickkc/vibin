package de.mickkc.vibin.routes

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.db.widgets.SharedWidgetEntity
import de.mickkc.vibin.dto.SuccessDto
import de.mickkc.vibin.dto.widgets.CreateWidgetDto
import de.mickkc.vibin.dto.widgets.WidgetDto
import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.ControlRepo
import de.mickkc.vibin.repos.ImageRepo
import de.mickkc.vibin.repos.WidgetRepo
import de.mickkc.vibin.utils.AccessCryptoUtil
import de.mickkc.vibin.utils.ChromeUtils
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
import io.ktor.server.routing.openapi.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

private data class WidgetColors(
    val backgroundColor: Int,
    val foregroundColor: Int,
    val accentColor: Int
)

private fun RoutingCall.parseWidgetColors(widget: SharedWidgetEntity): WidgetColors {
    val backgroundColor = request.queryParameters["bgColor"]?.let {
        WidgetUtils.colorFromHex(it.removePrefix("#"))
    } ?: widget.bgColor ?: 0x1D2021

    val foregroundColor = request.queryParameters["fgColor"]?.let {
        WidgetUtils.colorFromHex(it.removePrefix("#"))
    } ?: widget.fgColor ?: 0xEBDBB2

    val accentColor = request.queryParameters["accentColor"]?.let {
        WidgetUtils.colorFromHex(it.removePrefix("#"))
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
    layout: String,
    language: String,
    baseUrl: String? = null
): WidgetContext {
    return WidgetContext(
        widgetId = widget.id.value,
        userId = WidgetRepo.getUserId(widget),
        accentColor = colors.accentColor,
        backgroundColor = colors.backgroundColor,
        foregroundColor = colors.foregroundColor,
        layout = layout,
        language = language,
        baseUrl = baseUrl
    )
}

@OptIn(ExperimentalKtorApi::class)
fun Application.configureWidgetRoutes() = routing {

    get("/api/widgets/styles") {
        call.respondText(WidgetStyles.render(), contentType = ContentType.Text.CSS)
    }.describe {
        operationId = "getWidgetStyles"
        summary = "Get CSS styles for widgets"
        tag("Widgets")
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
            forbidden("Invalid signature")
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
    }.describe {
        operationId = "getImageForWidget"
        summary = "Get an image for a widget"
        description = "Because this endpoint is not authenticated, accessing images requires a signature and an expiration " +
                "timestamp. Checksums starting with `default-` (`default-album`, `default-artist`, `default-playlist`," +
                "`default-track` and `default-user`) return the default placeholder image of that type."
        tag("Widgets")
        parameters {
            p("checksum", "C7iUT7hEJH3fQ6vo8heyaRztGIs3hIe6", description = "Image checksum")
            q("quality", 192, required = false, description = "Width and height of the image")
            q("sig", "7QlNYUkZoFXnjcGR+2WLmw57JNDQXrQhIR7s3DfrtWY=", required = true,
                description = "Image signature")
            q("exp", 1782511260L, required = true,
                description = "Image signature expiration timestamp in UNIX seconds")
        }
        responses {
            successResponse<ByteArray>("image data")
            notFoundResponse("Image or file")
            forbiddenResponse("Invalid signature")
        }
    }

    getP("/api/widgets/{id}") {
        val id = call.getStringParameter("id")
        val widget = WidgetRepo.getWidget(id) ?: notFound()

        val baseUrl = call.parameters["baseUrl"]?.let {
            WidgetUtils.parseBaseUrl(it) ?: invalidParameter("baseUrl")
        }

        val colors = call.parseWidgetColors(widget)
        val language = call.parseLanguage()
        val layout = call.getStringOrDefault("layout", "col")
        val widgetTypes = getValidatedWidgetTypes(widget)
        val ctx = createWidgetContext(widget, colors, layout, language, baseUrl)

        val html = WidgetBuilder.build(widgetTypes, ctx)

        if (ctx.external) {
            call.response.header("X-Content-Type-Options", "nosniff")
            call.respondText(html, ContentType.Text.Plain)
        }
        else {
            call.respondText(html, ContentType.Text.Html)
        }
    }.describe {
        operationId = "getWidgetHTML"
        summary = "Get a widget as HTML"
        description = "When `baseUrl` is set, the widget is served as external. This means that it will not be displayed " +
                "as a HTML page, but in plain text to prevent loading scripts from malicious sources and URL redirection attacks."
        tag("Widgets")
        parameters {
            p("id", "vXSDbMiKk58XkIgJBE3JiwulsSTIpvmeJbQLKMo9qYp0IIKTzgqPpf6TbSBDfyiA")
            q("baseUrl", "https://vibin.example.com", required = false,
                description = "Server baseUrl for external embedding mode")
            q("layout", "col", required = false,
                description = "- `col`: Column\n- `row`: Row\n" +
                        "- `col-N`: `N` Columns\n- `row-N`: `N` Rows\n" +
                        "- `col-200px`: Columns that are max. 200px wide")
            q("lang", "en", required = false,
                description = "Widget language: ${WidgetBuilder.SUPPORTED_LANGUAGES.joinToString { "`${it}`" }}\n" +
                        "If not set, the language will be taken from the `Accept-Language` header.\n" +
                        "If the language is not supported, english is used as a fallback.")
            q("bgColor", "000000", required = false, description = "Background hex color, with or without (encoded) #.")
            q("fgColor", "ffffff", required = false, description = "Foreground hex color, with or without (encoded) #.")
            q("accentColor", "ff0000", required = false, description = "Accent hex color, with or without (encoded) #.")
        }
        responses {
            successResponse<String>("Widget HTML")
            notFoundResponse("Widget")
        }
    }

    getP("/api/widgets/{id}/image") {
        if (!ChromeUtils.isAvailable)
            notImplemented("Chromium is not available.")

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
        val layout = call.getStringOrDefault("layout", "col")
        val widgetTypes = getValidatedWidgetTypes(widget)
        val ctx = createWidgetContext(widget, colors, layout, language)

        val imageBytes = WidgetImageCache.getOrCreateImage(widget, widgetTypes, ctx, width, height)

        val imageCacheDuration = EnvUtil.getOrError(EnvUtil.WIDGET_CACHE_EXPIRATION_MINUTES)
        call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=${imageCacheDuration * 60}, stale-while-revalidate=60")

        call.respondBytes(imageBytes, ContentType.Image.PNG)
    }.describe {
        operationId = "getWidgetImage"
        summary = "Get a widget as an image"
        description = "Uses chromium and selenium to render a widget into an image."
        tag("Widgets")
        parameters {
            p("id", "vXSDbMiKk58XkIgJBE3JiwulsSTIpvmeJbQLKMo9qYp0IIKTzgqPpf6TbSBDfyiA")
            q("layout", "col", required = false,
                description = "- `col`: Column\n- `row`: Row\n" +
                        "- `col-N`: `N` Columns\n- `row-N`: `N` Rows\n" +
                        "- `col-200px`: Columns that are max. 200px wide")
            q("lang", "en", required = false,
                description = "Widget language: ${WidgetBuilder.SUPPORTED_LANGUAGES.joinToString { "`${it}`" }}\n" +
                        "If not set, the language will be taken from the `Accept-Language` header.\n" +
                        "If the language is not supported, english is used as a fallback.")
            q("bgColor", "000000", required = false, description = "Background hex color, with or without (encoded) #.")
            q("fgColor", "ffffff", required = false, description = "Foreground hex color, with or without (encoded) #.")
            q("accentColor", "ff0000", required = false, description = "Accent hex color, with or without (encoded) #.")
            q("width", 1080, required = false, description = "Image width")
            q("height", 720, required = false,
                description = "Image height. If null, the height is automatically determined by the width and the size of " +
                        "the widget. This is convient, but also slows down the rendering process a bit.")
        }
        responses {
            successResponse<ByteArray>("Widget Image")
            rateLimitedResponse()
            notFoundResponse("Widget")
        }
    }

    authenticate("tokenAuth") {

        getP("/api/widgets", PermissionType.MANAGE_WIDGETS) {
            val user = call.getUser() ?: unauthorized()

            val widgets = WidgetRepo.getAllForUser(user)
            val widgetDtos = WidgetRepo.toDto(widgets)

            call.respond(widgetDtos)
        }.describe {
            operationId = "getWidgets"
            summary = "Get all widgets shared by the current user"
            tag("Widgets")
            authResponses(PermissionType.MANAGE_WIDGETS) {
                successResponse<List<WidgetDto>>("A list of shared widgets")
            }
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
        }.describe {
            operationId = "createWidget"
            summary = "Create a new widget"
            description = "You must either specify all colors, or no colors."
            tag("Widgets")
            parameters {
                q("types", listOf(WidgetType.USER, WidgetType.SERVER_STATS), required = true,
                    description = "Enabled widget types. All types must be unique.")
                q("bgColor", "000000", required = false, description = "Background hex color, without #.")
                q("fgColor", "ffffff", required = false, description = "Foreground hex color, without #.")
                q("accentColor", "ff0000", required = false, description = "Accent hex color, without #.")
            }
            authResponses(PermissionType.MANAGE_WIDGETS) {
                successResponse<WidgetDto>("The created widget")
                invalidParameterResponse("colors", "Some colors where specified, while others weren't")
            }
        }

        deleteP("/api/widgets/{id}", PermissionType.MANAGE_WIDGETS) {
            val id = call.getStringParameter("id")
            val userId = call.getUserId() ?: unauthorized()

            val widget = WidgetRepo.getWidget(id) ?: notFound()

            val success = WidgetRepo.deleteWidget(widget, userId)
            if (!success) {
                forbidden("Widget is not owned by user")
            }

            // Evict cached images for this widget
            WidgetImageCache.evictCacheForWidget(id)

            call.success()
        }.describe {
            operationId = "deleteWidget"
            summary = "Delete a widget"
            tag("Widgets")
            parameters {
                p("id", "vXSDbMiKk58XkIgJBE3JiwulsSTIpvmeJbQLKMo9qYp0IIKTzgqPpf6TbSBDfyiA")
            }
            authResponses(PermissionType.MANAGE_WIDGETS) {
                successResponse<SuccessDto>("Success")
                forbiddenResponse("Widget is not owned by user")
                notFoundResponse("Widget")
            }
        }
    }

    staticResources("/api/js", "js")
        .describe {
            operationId = "getWidgetScripts"
            summary = "Contains scripts used in widgets"
            tag("Widgets")
        }

    webSocket("/api/widgets/{id}/listeningto/ws") {
        val id = call.parameters["id"]
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No Widget ID provided"))
        val widget = WidgetRepo.getWidget(id)
            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid widget ID"))

        if (!WidgetRepo.getTypes(widget).contains(WidgetType.LISTENING_TO)) {
            return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Widget does not contain the 'listening to' widget type."))
        }

        val userId = WidgetRepo.getUserId(widget)
        val baseUrl = call.parameters["baseUrl"]?.let {
            WidgetUtils.parseBaseUrl(it)
                ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid base URL"))
        }

        val session = ControlRepo.getCurrentSession(userId)
            ?: ControlRepo.getLatestSession(userId)
            ?: return@webSocket close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "No current or previous session for user."))


        val listener = ListeningToWidgetListener(session, userId, outgoing, baseUrl)

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
    }.describe {
        operationId = "getListeningToWidgetWS"
        summary = "Get a live feed of the current listening status"
        description = "See [Websocket docs](https://vibin.rip/docs/websockets#listening-to-widget)"
        tag("Tasks")
        parameters {
            p("id", "vXSDbMiKk58XkIgJBE3JiwulsSTIpvmeJbQLKMo9qYp0IIKTzgqPpf6TbSBDfyiA")
            q("baseUrl", "https://vibin.example.com", required = true,
                description = "The server base URL")
        }
    }
}