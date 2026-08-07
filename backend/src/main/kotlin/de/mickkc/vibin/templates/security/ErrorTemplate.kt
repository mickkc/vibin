package de.mickkc.vibin.templates.security

import de.mickkc.vibin.config.EnvUtil
import de.mickkc.vibin.config.EnvUtil.EXTERNAL_URL
import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.Template
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.img
import kotlinx.html.p

class ErrorTemplate(val errorKey: String, val refererHeader: String?) : Template<FlowContent> {

    val validatedRefererUrl = refererHeader?.takeIf {
        val externalUrl = EnvUtil.getOrNull(EXTERNAL_URL)
            ?: return@takeIf false
        return@takeIf it.startsWith(externalUrl)
    }

    override fun FlowContent.apply() {
        div("row") {
            img("Error icon", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAACiElEQVRIS7WV207UUBSG/78zyMmoj8AVEOKFRhQcNUHvCB0OMh0k6oXxvYxREkM7EnHqeEGUxATwGC6MASXRN9AY5Sizl9ltd+nAzCBEvpu916H771rZXSWOGMa7I2JfASkVzkKpKZBtEEABsMLQ1z/l7dyxofHF0KxOTQGZdjuQ4nKwjxLNugdhO7O5ldhOUD3f9x6CuBWcKNYaYF1lduRtnKDdM497saleAmjWdhm8n7Zzd0zcsEdAfHcKkJGwEdJNO/8hDlZBnrsXpcz56KAp2s5ouA2pEJCS2wXFT4GxJi3M59ejUF16593mhe9cC4yUdLI//zkK7RLwPQk6XZbLHBqbiwMR4rsCEhxw9lbueVfQrF7pymnvxOON+O45gO8B/KLtnDD+JFLyRF+j5AFJlF9YBVTLOtDdGrV2R6DkLUOhAyIZZvMLxp8krLC2gDx1r8HiC0CWaOe7tC8pIEoBqRoPa7SAVrD2ydGreYkdgX3eTiOlSYGq7PFuyr4n+v4dTuDZpOg7QHusdo6+CODhBP6FOgLuNwHbuI1LHHbmjf8gNBa9vg1iFoIVZp127UtWkIGFOSj8pO2cMv4k4nubeqXtNBpfEvHd3wBbIdLDbD4YLRXt0OUpEMfJ8+sDOf1NVCC+t6FX2k6T8RlkxuvDJmb1PtnmXQIPTgNNHwP3AUaFFIstihur4RhPddG+vhSFKgU04k9OA9ZgYCjrAgdH30WhqsiTQg/S8joy6w87g/jeBICbeggDDT+wvdXP4fE3cUKQ8yhDpEsCnNR2GZhI285tEzdUFdA0FAudW5S41Lqw3MGBG19iO0FNAUP0y7wH8EzkCR9TWMQW7zKXO9wv839x5AJ/Ae7WESjuLt4PAAAAEGRlQkdFNEEwQzMzNzI1RTVBNUNF2Gie/gAAAABJRU5ErkJggg==") {
                width = "24"
                height = "24"
            }
            h2 { +TemplateTranslator.getTranslation("web.security.error.title") }
        }
        p("desc") {
            +TemplateTranslator.getTranslation(errorKey)
        }
        if (validatedRefererUrl != null) {
            a(validatedRefererUrl, classes = "btn primary") {
                +TemplateTranslator.getTranslation("web.security.error.go_back")
            }
        }
    }
}