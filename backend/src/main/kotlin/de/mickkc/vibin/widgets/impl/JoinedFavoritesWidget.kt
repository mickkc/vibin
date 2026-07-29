package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import io.ktor.server.html.*
import kotlinx.html.FlowContent
import kotlinx.html.div

class JoinedFavoritesWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun FlowContent.apply() {
        div("widget-body") {

            div("joined-favorites") {
                insert(FavoriteTracksWidget(ctx)){}
                insert(FavoriteAlbumsWidget(ctx)){}
                insert(FavoriteArtistsWidget(ctx)){}
            }
        }
    }
}