package de.mickkc.vibin.templates

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.SERVER_LANGUAGE
import java.util.Locale
import java.util.ResourceBundle

object TemplateTranslator {

    val language = EnvUtil.getOrError(SERVER_LANGUAGE)

    private val supportedTemplateLanguages = setOf("de", "en", "ro")
    private var resourceBundle: ResourceBundle? = null

    init {
        if (language !in supportedTemplateLanguages)
            throw Exception("The language '$language' is not supported for Templates. Supported languages: ${supportedTemplateLanguages.joinToString()}")
    }


    fun getTranslation(key: String, vararg args: Any?): String {
        val bundle = getResourceBundle()
        return String.format(bundle.getString(key), *args)
    }

    private fun getResourceBundle(): ResourceBundle {
        if (resourceBundle != null) {
            return resourceBundle!!
        }

        resourceBundle = ResourceBundle.getBundle("messages_templates", Locale.forLanguageTag(language))
        return resourceBundle!!
    }
}