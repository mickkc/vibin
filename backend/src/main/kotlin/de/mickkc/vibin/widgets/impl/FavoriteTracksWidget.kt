package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.itemSection
import kotlinx.html.FlowContent
import kotlinx.html.div

class FavoriteTracksWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun FlowContent.apply() {
        div("widget-body") {

            val favorites = FavoriteRepo.getFavoriteTracksForUser(ctx.userId)

            itemSection(
                this@FavoriteTracksWidget,
                title = t("widgets.favorites.tracks.title"),
                items = favorites,
                getType = {"track"},
                getCover = { TrackRepo.getCover(it) },
                getTitle = { it.title },
                getSubtitle = { TrackRepo.getArtists(it).joinToString { it.name } },
                showPlace = true
            )
        }
    }
}