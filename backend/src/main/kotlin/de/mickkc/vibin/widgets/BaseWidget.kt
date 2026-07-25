package de.mickkc.vibin.widgets

import io.ktor.server.html.Template
import kotlinx.html.FlowContent
import java.util.*

abstract class BaseWidget(val ctx: WidgetContext) : Template<FlowContent> {

    var interactive = true
    private var resourceBundle: ResourceBundle? = null

    val backgroundColor: String
        get() = WidgetUtils.colorToHex(ctx.backgroundColor)

    val foregroundColor: String
        get() = WidgetUtils.colorToHex(ctx.foregroundColor)

    val accentColor: String
        get() = WidgetUtils.colorToHex(ctx.accentColor)

    fun t(key: String, vararg args: Any?): String {
        val bundle = getResourceBundle()
        return String.format(bundle.getString(key), *args)
    }

    private fun getResourceBundle(): ResourceBundle {
        if (resourceBundle != null) {
            return resourceBundle!!
        }

        resourceBundle = ResourceBundle.getBundle("messages_widgets", Locale.forLanguageTag(ctx.language))
        return resourceBundle!!
    }
}