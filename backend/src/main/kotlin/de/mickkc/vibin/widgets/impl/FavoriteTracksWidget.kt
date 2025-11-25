package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.favoritesSection
import kotlinx.html.div
import kotlinx.html.stream.appendHTML

class FavoriteTracksWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(interactive: Boolean): String = buildString {
        appendHTML(prettyPrint = false).div("widget-body") {

            val favorites = FavoriteRepo.getFavoriteTracksForUser(ctx.userId)

            favoritesSection(
                this@FavoriteTracksWidget,
                title = t("widgets.favorites.tracks.title"),
                favorites = favorites,
                type = "track",
                getCover = { TrackRepo.getCover(it) },
                getTitle = { it.title },
                getSubtitle = { TrackRepo.getArtists(it).joinToString { it.name } }
            )

        }
    }
}