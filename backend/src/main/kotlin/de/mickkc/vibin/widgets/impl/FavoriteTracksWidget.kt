package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.utils.PathUtils
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.favoritesSection
import kotlinx.html.div
import kotlinx.html.stream.appendHTML

class FavoriteTracksWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(): String = buildString {
        appendHTML().div("widget-body") {

            val favorites = FavoriteRepo.getFavoriteTracksForUser(ctx.userId)

            favoritesSection(
                this@FavoriteTracksWidget,
                title = t("widgets.favorites.tracks.title"),
                favorites = favorites,
                getCover = {
                    TrackRepo.getCover(it)?.let {
                        ImageCache.getImageFile(it, 128)
                    }
                    ?: PathUtils.getDefaultImage("track", 128)
               },
                getTitle = { it.title },
                getSubtitle = { TrackRepo.getArtists(it).joinToString { it.name } }
            )

        }
    }
}