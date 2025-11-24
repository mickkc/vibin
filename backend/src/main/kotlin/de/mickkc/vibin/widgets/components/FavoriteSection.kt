package de.mickkc.vibin.widgets.components

import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*

fun<T> FlowContent.favoritesSection(
    widget: BaseWidget,
    title: String,
    favorites: List<T?>,
    type: String,
    getCover: (T) -> ImageEntity?,
    getTitle: (T) -> String,
    getSubtitle: (T) -> String,
) {
    div("favorites") {
        h2 {
            +title
        }
        div("favorite-items") {
            for ((index, item) in favorites.withIndex()) {
                val cover = item?.let { getCover(it) }
                val itemTitle = item?.let { getTitle(it) } ?: ""
                val itemSubtitle = item?.let { getSubtitle(it) } ?: ""

                val imageUrl = if (item == null) null else WidgetUtils.getImageUrl(cover, type, 128)

                val cardBg = WidgetUtils.blendColors(widget.ctx.backgroundColor, widget.ctx.accentColor, 0.2f)

                div("favorite-item") {
                    style = "background-color: ${WidgetUtils.colorToHex(cardBg)};"

                    if (imageUrl != null) {
                        img(classes = "item-cover") {
                            src = imageUrl
                            alt = itemTitle
                        }
                    }
                    else {
                        div("item-cover") {
                            style = "background-color: ${widget.accentColor}33;"
                        }
                    }

                    div("item-info") {
                        h3 {
                            this.title = itemTitle
                            +itemTitle
                        }
                        p("item-subtitle") {
                            this.title = itemSubtitle
                            +itemSubtitle
                        }
                    }

                    p("favorite-place") {
                        style = "background-color: ${widget.backgroundColor}aa; color: ${widget.accentColor};"

                        when (index) {
                            0 -> +"🥇"
                            1 -> +"🥈"
                            2 -> +"🥉"
                            else -> +"#${index + 1}"
                        }
                    }
                }
            }
        }
    }
}