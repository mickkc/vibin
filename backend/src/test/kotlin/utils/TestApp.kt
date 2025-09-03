package utils

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking

fun testApp(test: suspend TestApplicationBuilder.(HttpClient) -> Unit) = testApplication {
    initTestDb()
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

    runBlocking {
        test(client)
    }
}