package de.mickkc.vibin.widgets.impl

import de.mickkc.vibin.repos.AlbumRepo
import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.components.favoritesSection
import kotlinx.html.div
import kotlinx.html.stream.appendHTML

class FavoriteAlbumsWidget(ctx: WidgetContext) : BaseWidget(ctx) {

    override fun render(interactive: Boolean): String = buildString {
        appendHTML(prettyPrint = false).div("widget-body") {

            val favorites = FavoriteRepo.getFavoriteAlbumsForUser(ctx.userId)

            favoritesSection(
                this@FavoriteAlbumsWidget,
                title = t("widgets.favorites.albums.title"),
                favorites = favorites,
                type = "album",
                getCover = { AlbumRepo.getAlbumCover(it) },
                getTitle = { it.title },
                getSubtitle = {
                    it.description.ifBlank {
                        it.releaseYear?.toString()
                            ?: t("widgets.favorites.albums.subtitle_placeholder")
                    }
                }
            )
        }
    }
}