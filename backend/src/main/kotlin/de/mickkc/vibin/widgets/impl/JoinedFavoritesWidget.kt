package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import kotlinx.html.div
import kotlinx.html.stream.appendHTML
import kotlinx.html.unsafe

class JoinedFavoritesWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(): String = buildString {
        appendHTML(prettyPrint = false).div("widget-body") {

            div("joined-favorites") {
                unsafe {
                    +FavoriteTracksWidget(ctx).render()
                    +FavoriteAlbumsWidget(ctx).render()
                    +FavoriteArtistsWidget(ctx).render()
                }
            }
        }
    }
}