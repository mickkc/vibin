package de.mickkc.vibin.widgets.components

import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun<T> FlowContent.favoritesSection(
    widget: BaseWidget,
    title: String,
    favorites: List<T?>,
    getCover: (T) -> File?,
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

                val bytes = cover?.readBytes()
                val base64Image = if (bytes != null) {
                    "data:image/${cover.extension};base64,${
                        Base64.encode(bytes)
                    }"
                } else {
                    null
                }

                val cardBg = WidgetUtils.blendColors(widget.ctx.backgroundColor, widget.ctx.accentColor, 0.2f)

                div("favorite-item") {
                    style = "background-color: ${WidgetUtils.colorToHex(cardBg)};"

                    if (base64Image != null) {
                        img(classes = "item-cover") {
                            src = base64Image
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