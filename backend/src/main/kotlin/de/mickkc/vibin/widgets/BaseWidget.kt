package de.mickkc.vibin.widgets

import java.util.*

abstract class BaseWidget(val ctx: WidgetContext) {

    private var resourceBundle: ResourceBundle? = null

    abstract fun render(): String

    val backgroundColor: String
        get() = WidgetUtils.colorToHex(ctx.backgroundColor)

    val foregroundColor: String
        get() = WidgetUtils.colorToHex(ctx.foregroundColor)

    val accentColor: String
        get() = WidgetUtils.colorToHex(ctx.accentColor)

    fun t(key: String, vararg args: Any?): String {
        val bundle = getResourceBundle()
        return String.format(bundle.getString(key), args)
    }

    private fun getResourceBundle(): ResourceBundle {
        if (resourceBundle != null) {
            return resourceBundle!!
        }

        resourceBundle = ResourceBundle.getBundle("messages_widgets", Locale.forLanguageTag(ctx.language))
        return resourceBundle!!
    }
}