package de.mickkc.vibin.utils.http

import de.mickkc.vibin.utils.BuildInfo
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*

object UserAgentBuilder {

    fun buildUserAgent(useCase: String): String {
        return "Vibin-$useCase-Bot/${BuildInfo.APP_VERSION} (Ktor/${BuildInfo.KTOR_VERSION}) +https://codeberg.org/mickkc/vibin/raw/branch/main/BOT_DISCLOSURE.md"
    }

    fun configureUserAgent(config: HttpClientConfig<CIOEngineConfig>, useCase: String) {
        config.install(UserAgent) {
            agent = buildUserAgent(useCase)
        }
    }
}