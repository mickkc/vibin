package de.mickkc.vibin.widgets.components

import de.mickkc.vibin.widgets.BaseWidget
import de.mickkc.vibin.widgets.WidgetUtils
import kotlinx.html.*

fun FlowContent.statCard(widget: BaseWidget, value: String, label: String) {

    val statBg = WidgetUtils.blendColors(widget.ctx.backgroundColor, widget.ctx.accentColor, 0.2f)

    div("stat-card") {
        style = "background-color: ${WidgetUtils.colorToHex(statBg)};"

        p {
            style = "font-size: 3rem; color: ${widget.accentColor}; font-weight: bold;"
            +value
        }

        p {
            +label
        }
    }
}