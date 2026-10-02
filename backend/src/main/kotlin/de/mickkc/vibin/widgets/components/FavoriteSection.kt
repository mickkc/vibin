package de.mickkc.vibin.widgets.components

import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*

fun<T> FlowContent.itemSection(
    widget: BaseWidget,
    ctx: WidgetContext,
    title: String,
    items: List<T?>,
    getType: (T) -> String,
    getCover: (T) -> ImageEntity?,
    getTitle: (T) -> String,
    getSubtitle: (T) -> String,
    showPlace: Boolean = false
) {
    div("favorites") {
        h2 {
            +title
        }
        div("items") {
            for ((index, item) in items.withIndex()) {
                val cover = item?.let { getCover(it) }
                val itemTitle = item?.let { getTitle(it) } ?: ""
                val itemSubtitle = item?.let { getSubtitle(it) } ?: ""
                val type = item?.let { getType(it) } ?: ""

                val imageUrl = if (item == null) null else WidgetUtils.getImageUrl(cover, type, 128, ctx.external)

                val cardBg = WidgetUtils.blendColors(widget.ctx.backgroundColor, widget.ctx.accentColor, 0.2f)

                div("item") {
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

                    if (showPlace && index < 3) {
                        p("item-place") {
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
}