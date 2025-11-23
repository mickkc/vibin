package de.mickkc.vibin.routes

import de.mickkc.vibin.widgets.WidgetBuilder
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetStyles
import de.mickkc.vibin.widgets.WidgetType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureWidgetRoutes() = routing {

    get("/api/widgets/styles") {
        call.respondText(WidgetStyles.render(), contentType = ContentType.Text.CSS)
    }

    //authenticate("tokenAuth") {

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
    //}
}