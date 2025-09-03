package utils

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking

fun testApp(test: suspend TestApplicationBuilder.(HttpClient) -> Unit) = testApplication {
    application {
        testModule()
    }
    client = createClient {
        install(ContentNegotiation) {
            json()
        }
        defaultRequest {
            bearerAuth(testToken)
            contentType(ContentType.Application.Json)
        }
    }

    // Wait for the server to be ready before running tests
    application.monitor.subscribe(ServerReady) {
        runBlocking { test(client) }
    }
}