package de.mickkc.vibin.config

import de.mickkc.vibin.config.EnvUtil.CORS_ADDITIONAL_ALLOWED_HEADERS
import de.mickkc.vibin.config.EnvUtil.CORS_ADDITIONAL_ALLOWED_METHODS
import de.mickkc.vibin.config.EnvUtil.CORS_ALLOWED_HOSTS
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.configureCors() {
    val corsHosts = EnvUtil.getOrError(CORS_ALLOWED_HOSTS)
    if (corsHosts.isNotEmpty() && !corsHosts.equals("none", ignoreCase = true)) {
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)

            corsHosts
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { host ->
                    if (host == "*") {
                        anyHost()
                        return@forEach
                    }
                    val parts = host.trimEnd('/').split("://", limit = 2)
                    if (parts.size == 2) {
                        allowHost(parts[1], schemes = listOf(parts[0].lowercase()))
                    } else {
                        allowHost(host.trimEnd('/'), schemes = listOf("http", "https"))
                    }
                }

            EnvUtil.getOrNull(CORS_ADDITIONAL_ALLOWED_HEADERS)
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.forEach { header ->
                    allowHeader(header)
                }

            EnvUtil.getOrNull(CORS_ADDITIONAL_ALLOWED_METHODS)
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.forEach { method ->
                    val parsed = HttpMethod.parse(method.uppercase())
                    allowMethod(parsed)
                }

            allowCredentials = false
        }
    }

}