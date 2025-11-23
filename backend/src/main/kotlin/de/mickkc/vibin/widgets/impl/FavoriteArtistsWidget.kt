package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.repos.ArtistRepo
import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.utils.PathUtils
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
                getCover = {
                    ArtistRepo.getImage(it)?.let {
                        ImageCache.getImageFile(it, 128)
                    }
                    ?: PathUtils.getDefaultImage("artist", 128)
               },
                getTitle = { it.name },
                getSubtitle = { it.description.ifEmpty { t("widgets.favorites.artists.subtitle_placeholder") } }
            )
        }
    }
}