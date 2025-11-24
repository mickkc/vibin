package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.favoritesSection
import kotlinx.html.div
import kotlinx.html.stream.appendHTML

class FavoriteArtistsWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(): String = buildString {
        appendHTML(prettyPrint = false).div("widget-body") {

            val favorites = FavoriteRepo.getFavoriteArtistsForUser(ctx.userId)

            favoritesSection(
                this@FavoriteArtistsWidget,
                title = t("widgets.favorites.artists.title"),
                favorites = favorites,
                type = "artist",
                getCover = { ArtistRepo.getImage(it) },
                getTitle = { it.name },
                getSubtitle = { it.description.ifEmpty { t("widgets.favorites.artists.subtitle_placeholder") } }
            )
        }
    }
}