package rest

import de.mickkc.vibin.db.SessionEntity
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import utils.testApp
import de.mickkc.vibin.dto.LoginResultDto
import de.mickkc.vibin.repos.SessionRepo
import de.mickkc.vibin.utils.DateTimeUtils
import io.ktor.client.statement.bodyAsText
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class AuthRestTest {

    @Test
    fun testLogin() = testApp { client ->
        val response = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
            parameter("deviceId", "test")
        }
        assertTrue(response.status.isSuccess())

        val responseBody = response.body<LoginResultDto>()

        assertTrue(responseBody.success)
        assertTrue(responseBody.token.isNotBlank())
        assertEquals("Admin", responseBody.user.username)
    }

    @Test
    fun testLogin_Failure() = testApp { client ->
        val response = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "wrongpassword")
            parameter("deviceId", "test")
        }
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testLogin_DeviceIdTooLong_Failure() = testApp { client ->
        val response = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
            parameter("deviceId", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")
        }
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.BadRequest, response.status)

        val bodyText = response.bodyAsText()
        assertContains(bodyText, "\"parameterName\":\"deviceId\"")
    }

    @Test
    fun testLogin_NoDeviceId_Failure() = testApp { client ->
        val response = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
        }
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun logout() = testApp {client ->

        assertEquals(1, SessionRepo.count()) // one active session (default admin user)

        val response = client.post("/api/auth/logout")
        assertTrue(response.status.isSuccess())

        val responseBody = response.body<Map<String, Boolean>>()
        assertTrue(responseBody["success"] == true)

        assertEquals(0, SessionRepo.count())
    }

    @Test
    fun validateSession() = testApp { client ->

        val response = client.post("/api/auth/validate")
        assertTrue(response.status.isSuccess())

        val responseBody = response.body<LoginResultDto>()

        assertTrue(responseBody.success)
        assertTrue(responseBody.token.isNotBlank())
        assertEquals("Admin", responseBody.user.username)
    }

    @Test
    fun validateSession_ExpiredSession() = testApp { client ->

        // Make session expired (last used more than 1 week ago)
        transaction {
            SessionEntity.all().forEach {
                it.lastUsed = DateTimeUtils.now() - 21.days.inWholeSeconds
            }
        }

        val response = client.post("/api/auth/validate")
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun validateSession_Failure() = testApp { client ->

        // Logout first to invalidate the session
        client.post("/api/auth/logout")

        val response = client.post("/api/auth/validate")
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}