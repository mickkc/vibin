package parsers

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertDoesNotThrow
import utils.WidgetTestUtils
import utils.testApp
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenApiDocsTest {

    @Test
    fun testOpenApiDocs_ServesJson() = testApp { client ->

        val response = client.get("/api/docs/documentation.json")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json.toString(), response.headers[HttpHeaders.ContentType])

        val text = response.bodyAsText()
        assertDoesNotThrow { Json.parseToJsonElement(text) }
    }

    @Test
    fun testOpenApiDocs_ServesHtml() = testApp { client ->

        val response = client.get("/api/docs")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html.withParameter("charset", "UTF-8").toString(), response.headers[HttpHeaders.ContentType])

        val text = response.bodyAsText()
        val doc = assertDoesNotThrow { WidgetTestUtils.parseHtml(text) }

        assertEquals("Swagger UI", doc.title())
    }
}