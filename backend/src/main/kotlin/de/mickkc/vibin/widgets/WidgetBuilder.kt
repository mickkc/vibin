package de.mickkc.vibin.widgets

import de.mickkc.vibin.widgets.impl.*
import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML
import kotlin.math.ceil

object WidgetBuilder {

    val SUPPORTED_LANGUAGES = listOf("en", "de", "ro")

    private val fixedRowStyleRegex = Regex("row-[0-9]+")
    private val fixedColStyleRegex = Regex("col-[0-9]+")
    private val colSizeStyleRegex = Regex("col-[0-9]+px")

    private fun getLayoutStyle(layout: String, widgetAmount: Int): String? {
        return when (layout.lowercase()) {
            "col" -> "display:flex;flex-direction:column;"
            "row" -> "display:flex;flex-direction:row;"
            else -> {
                if (fixedRowStyleRegex.matches(layout.lowercase())) {
                    val rows = layout.substringAfterLast("-").toIntOrNull() ?: return null
                    "display:grid;grid-template-rows:repeat($rows,auto);grid-template-columns:repeat(${ceil(widgetAmount.toFloat() / rows).toInt()},1fr);align-content:start;"
                }
                else if (fixedColStyleRegex.matches(layout.lowercase())) {
                    val cols = layout.substringAfterLast("-").toIntOrNull() ?: return null
                    "display:grid;grid-template-columns:repeat($cols,1fr);align-content:start;"
                }
                else if (colSizeStyleRegex.matches(layout.lowercase())) {
                    val colWidth = layout.substringAfterLast("-").removeSuffix("px").toIntOrNull() ?: return null
                    "display:grid;grid-template-columns:repeat(auto-fit,minmax(${colWidth}px,1fr));align-content:start;"
                }
                else null
            }
        }
    }

    fun build(types: List<WidgetType>, ctx: WidgetContext, interactive: Boolean = true): String {

        val widgets = types.map<WidgetType, BaseWidget> { type ->
            when (type) {
                WidgetType.USER -> UserWidget(ctx)
                WidgetType.ACTIVITY -> ActivityWidget(ctx)
                WidgetType.SIMPLE_ACTIVITY -> ActivityWidget(ctx, extended = false)
                WidgetType.FAVORITE_TRACKS -> FavoriteTracksWidget(ctx)
                WidgetType.FAVORITE_ALBUMS -> FavoriteAlbumsWidget(ctx)
                WidgetType.FAVORITE_ARTISTS -> FavoriteArtistsWidget(ctx)
                WidgetType.JOINED_FAVORITES -> JoinedFavoritesWidget(ctx)
                WidgetType.SERVER_STATS -> ServerStatsWidget(ctx)
                WidgetType.LISTENING_TO -> ListeningToWidget(ctx)
                WidgetType.RECENT_TRACKS -> RecentTracksWidget(ctx)
                WidgetType.RECENT_ALBUMS -> RecentAlbumsWidget(ctx)
                WidgetType.RECENT_ARTISTS -> RecentArtistsWidget(ctx)
                WidgetType.RECENTLY_LISTENED -> RecentlyListenedWidget(ctx)
                WidgetType.MOST_LISTENED_TRACKS -> MostListenedTracksWidget(ctx)
                WidgetType.MOST_LISTENED_ALBUM -> MostListenedAlbumsWidget(ctx)
                WidgetType.MOST_LISTENED_ARTISTS -> MostListenedArtistsWidget(ctx)
                WidgetType.MOST_LISTENED_TO -> MostListenedWidget(ctx)
            }.also { it.interactive = interactive }
        }

        val bgColor = WidgetUtils.colorToHex(ctx.backgroundColor)
        val fgColor = WidgetUtils.colorToHex(ctx.foregroundColor)
        val accentColor = WidgetUtils.colorToHex(ctx.accentColor)

        fun HtmlBlockTag.applyBody() {
            val bodyStyle = getLayoutStyle(ctx.layout, widgets.size)
            style = "margin: 0; padding: 0; background-color: $bgColor; color: $fgColor; accent-color: $accentColor; ${bodyStyle ?: ""}"
            widgets.forEach { widget ->
                insert(widget) {}

                // only show separators in column layout
                if (widget != widgets.last() && (bodyStyle == null || bodyStyle.contains("flex-direction:column"))) {
                    div {
                        style = "width: 100%; height: 1px; background-color: ${fgColor}33; margin: 1rem 0;"
                    }
                }
            }
        }

        if (ctx.external) {
            return createHTML().div("vibin-widget-container") {
                applyBody()
            }
        }

        return buildString {
            appendHTML(prettyPrint = false).html {
                style = "background: $bgColor;"

                lang = ctx.language

                head {
                    title { +"Vibin' Widget" }
                    meta("charset", "UTF-8")
                    meta("viewport", "width=device-width, initial-scale=1.0")
                    link(rel = "stylesheet", href = "/api/widgets/styles", type = "text/css")
                    style {
                        unsafe {
                            +"""
                                html {-ms-overflow-style:none;scrollbar-width:none;}
                                *::-webkit-scrollbar {display:none;}
                            """.trimIndent()
                        }
                    }
                }

                body("vibin-widget-container") {
                    applyBody()
                }
            }
        }
    }
}