package de.mickkc.vibin.widgets

import de.mickkc.vibin.widgets.impl.ActivityWidget
import de.mickkc.vibin.widgets.impl.FavoriteAlbumsWidget
import de.mickkc.vibin.widgets.impl.FavoriteArtistsWidget
import de.mickkc.vibin.widgets.impl.FavoriteTracksWidget
import de.mickkc.vibin.widgets.impl.JoinedFavoritesWidget
import de.mickkc.vibin.widgets.impl.ServerStatsWidget
import de.mickkc.vibin.widgets.impl.UserWidget
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

object WidgetBuilder {

    val SUPPORTED_LANGUAGES = listOf("en", "de")

    fun build(types: List<WidgetType>, ctx: WidgetContext): String {

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
            }
        }

        val bgColor = WidgetUtils.colorToHex(ctx.backgroundColor)
        val fgColor = WidgetUtils.colorToHex(ctx.foregroundColor)

        return buildString {
            appendHTML(prettyPrint = false).html {

                lang = ctx.language

                head {
                    title { +"Vibin' Widget" }
                    meta("charset", "UTF-8")
                    meta("viewport", "width=device-width, initial-scale=1.0")
                    link(rel = "stylesheet", href = "/api/widgets/styles", type = "text/css")
                }

                body {
                    style = "margin: 0; padding: 0; background-color: $bgColor; color: $fgColor;"
                    widgets.forEach { widget ->
                        unsafe {
                            +widget.render()
                        }
                        if (widget != widgets.last()) {
                            div {
                                style = "width: 100%; height: 1px; background-color: ${fgColor}33; margin: 1rem 0;"
                            }
                        }
                    }

                    div {
                        style = "position: fixed; bottom: 5px; right: 5px; font-size: 10px; color: ${fgColor}66;"
                        +"Powered by "
                        a(href = "https://github.com/mickkc/vibin", target = "_blank") {
                            rel = "noopener noreferrer"
                            style = "color: ${fgColor}66;"
                            +"Vibin'"
                        }
                    }
                }
            }
        }

    }
}