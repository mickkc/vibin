package de.mickkc.vibin.widgets

import de.mickkc.vibin.widgets.impl.*
import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.html.stream.appendHTML

object WidgetBuilder {

    val SUPPORTED_LANGUAGES = listOf("en", "de")

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
                    style = "margin: 0; padding: 0; background-color: $bgColor; color: $fgColor; accent-color: $accentColor"
                    widgets.forEach { widget ->
                        insert(widget) {}
                        if (widget != widgets.last()) {
                            div {
                                style = "width: 100%; height: 1px; background-color: ${fgColor}33; margin: 1rem 0;"
                            }
                        }
                    }
                }
            }
        }
    }
}