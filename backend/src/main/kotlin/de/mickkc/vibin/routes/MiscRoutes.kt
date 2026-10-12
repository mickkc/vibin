package de.mickkc.vibin.routes

import de.mickkc.vibin.mail.MailSender
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.server.WelcomeTexts
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri

fun Application.configureMiscRoutes() = routing {

    get("/api/misc/welcome") {
        val texts = Settings.get(WelcomeTexts)
        if (texts.isNotEmpty()) {
            val randomText = texts.random()
            call.respond(randomText)
        } else {
            call.respond("")
        }
    }

    route("/api/echo") {
        handle {
            val route = call.request.uri
            val method = call.request.httpMethod
            val queryParams = call.request.queryParameters
            val headers = call.request.headers
            val ip = call.request.local.remoteAddress
            val port = call.request.local.remotePort

            call.respondText(buildString {
                appendLine("=== Route:")
                appendLine(route)

                appendLine("\n=== Method:")
                appendLine(method.value)

                appendLine("\n=== Parameters:")
                if (queryParams.isEmpty()) {
                    appendLine("<none>")
                }
                else {
                    queryParams.forEach { name, values ->
                        append(name)
                        append(": ")
                        appendLine(values.joinToString(", "))
                    }
                }

                appendLine("\n=== Headers:")
                if (headers.isEmpty()) {
                    appendLine("<none>")
                }
                else {
                    headers.forEach { name, values ->
                        append(name)
                        append(": ")
                        appendLine(values.joinToString(", "))
                    }
                }

                appendLine("\n=== IP, Port:")
                appendLine(ip)
                appendLine(port)
            }, ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }

    authenticate("tokenAuth") {
        get("/api/mail/supported") {
            call.success(MailSender.configured)
        }
    }

}