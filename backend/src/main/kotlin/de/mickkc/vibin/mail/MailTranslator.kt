package de.mickkc.vibin.mail

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.EMAIL_LANGUAGE
import java.util.Locale
import java.util.ResourceBundle

object MailTranslator {

    val language = EnvUtil.getOrError(EMAIL_LANGUAGE)

    private val supportedMailLanguages = setOf("de", "en")
    private var resourceBundle: ResourceBundle? = null

    init {
        if (language !in supportedMailLanguages)
            throw Exception("The language '$language' is not supported for Emails. Supported languages: ${supportedMailLanguages.joinToString()}")
    }


    fun getTranslation(key: String, vararg args: Any?): String {
        val bundle = getResourceBundle()
        return String.format(bundle.getString(key), *args)
    }

    private fun getResourceBundle(): ResourceBundle {
        if (resourceBundle != null) {
            return resourceBundle!!
        }

        resourceBundle = ResourceBundle.getBundle("messages_mails", Locale.forLanguageTag(language))
        return resourceBundle!!
    }
}