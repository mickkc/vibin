package de.mickkc.vibin.routes

import de.mickkc.vibin.dto.SuccessDto
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
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Application.configureMiscRoutes() = routing {

    get("/api/misc/welcome") {
        val texts = Settings.get(WelcomeTexts)
        if (texts.isNotEmpty()) {
            val randomText = texts.random()
            call.respond(randomText)
        } else {
            call.respond("")
        }
    }.describe {
        operationId = "getWelcomeMessage"
        description = "Get a welcome message"
        description = "Returns a random welcome message configured by the server administrator."
        tag("Misc")
        responses {
            successResponse<String>("The welcome message, or an empty string in case there are no welcome " +
                    "messages configured.", exampleValue = "Welcome!")
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
    }.describe {
        operationId = "echo"
        summary = "Echo"
        description = "Responds with data about your request; route, method, parameters, headers, etc."
        tag("Misc")
        responses {
            successResponse("Data about the request.", "=== Route:\n/api/echo\n\n=== Method:\nGET\n\n...")
        }
    }

    authenticate("tokenAuth") {
        get("/api/mail/supported") {
            call.success(MailSender.configured)
        }.describe {
            operationId = "checkMailSupport"
            summary = "Check if sending mails is supported"
            tag("Misc")
            authResponses {
                successResponse<SuccessDto>("True if mails can be sent, false if not.")
            }
        }
    }

}