package rest

import de.mickkc.vibin.config.EnvUtil
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
            parameter("deviceName", "Test Device")
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
            parameter("deviceName", "Test Device")
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
            parameter("deviceName", "Test Device")
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
            parameter("deviceName", "Test Device")
        }
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun testLogin_NoDeviceName_Failure() = testApp { client ->
        val response = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
            parameter("deviceId", "test")
        }
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.BadRequest, response.status)

        val bodyText = response.bodyAsText()
        assertContains(bodyText, "\"parameterName\":\"deviceName\"")
    }

    @Test
    fun testLogin_UnknownUser_SameResponseAsWrongPassword() = testApp { client ->
        suspend fun login(username: String) = client.post("/api/auth/login") {
            parameter("username", username)
            parameter("password", "wrongpassword")
            parameter("deviceId", "test")
            parameter("deviceName", "Test Device")
        }
        val known = login("Admin")
        val unknown = login("DoesNotExist")

        assertEquals(HttpStatusCode.Unauthorized, unknown.status)
        assertEquals(known.status, unknown.status)
        assertEquals(known.bodyAsText(), unknown.bodyAsText())
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

    // region Rate Limiting

    @Test
    fun testLoginRateLimit_ExceededAfterMaxFailedAttempts() {
        EnvUtil.addOverride(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS, "3")
        try {
            testApp(false) { client ->
                // Make 3 failed login attempts
                repeat(3) {
                    val response = client.post("/api/auth/login") {
                        parameter("username", "Admin")
                        parameter("password", "wrongpassword")
                        parameter("deviceId", "test")
                        parameter("deviceName", "Test Device")
                    }
                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }

                // 4th attempt should be rate limited
                val response = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                }
                assertEquals(HttpStatusCode.TooManyRequests, response.status)
            }
        } finally {
            EnvUtil.overrides.remove(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS.name)
        }
    }

    @Test
    fun testLoginRateLimit_SuccessfulLoginDoesNotCountTowardLimit() {
        EnvUtil.addOverride(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS, "2")
        try {
            testApp(false) { client ->
                // Failed attempt 1 (count = 1)
                val r1 = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                }
                assertEquals(HttpStatusCode.Unauthorized, r1.status)

                // Successful login (count stays at 1)
                val r2 = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "admin")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                }
                assertTrue(r2.status.isSuccess())

                // Failed attempt 2 (count = 2)
                val r3 = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                }
                assertEquals(HttpStatusCode.Unauthorized, r3.status)

                // Next attempt should be rate limited
                val r4 = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                }
                assertEquals(HttpStatusCode.TooManyRequests, r4.status)
            }
        } finally {
            EnvUtil.overrides.remove(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS.name)
        }
    }

    @Test
    fun testLoginRateLimit_PerRealIpHeader() {
        EnvUtil.addOverride(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS, "2")
        EnvUtil.addOverride(EnvUtil.REAL_IP_HEADER, "X-Forwarded-For")
        try {
            testApp(false) { client ->
                // Exhaust rate limit for IP 1.2.3.4
                repeat(2) {
                    val response = client.post("/api/auth/login") {
                        parameter("username", "Admin")
                        parameter("password", "wrongpassword")
                        parameter("deviceId", "test")
                        parameter("deviceName", "Test Device")
                        header("X-Forwarded-For", "1.2.3.4")
                    }
                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }

                // IP 1.2.3.4 should now be rate limited
                val rateLimitedResponse = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Forwarded-For", "1.2.3.4")
                }
                assertEquals(HttpStatusCode.TooManyRequests, rateLimitedResponse.status)

                // IP 5.6.7.8 should still be allowed
                val allowedResponse = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Forwarded-For", "5.6.7.8")
                }
                assertEquals(HttpStatusCode.Unauthorized, allowedResponse.status)
            }
        } finally {
            EnvUtil.overrides.remove(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS.name)
            EnvUtil.overrides.remove(EnvUtil.REAL_IP_HEADER.name)
        }
    }

    @Test
    fun testLoginRateLimit_XForwardedFor() {
        EnvUtil.addOverride(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS, "2")
        EnvUtil.addOverride(EnvUtil.REAL_IP_HEADER, "X-Forwarded-For")
        try {
            testApp(false) { client ->
                // Exhaust rate limit for IP 1.2.3.4 using X-Forwarded-For with multiple IPs
                repeat(2) {
                    val response = client.post("/api/auth/login") {
                        parameter("username", "Admin")
                        parameter("password", "wrongpassword")
                        parameter("deviceId", "test")
                        parameter("deviceName", "Test Device")
                        header("X-Forwarded-For", "1.2.3.4, 5.6.7.8, 9.10.11.12")
                    }
                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }

                // Should be rate limited (first IP 1.2.3.4 is the client ID)
                val rateLimitedResponse = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Forwarded-For", "1.2.3.4, 5.6.7.8")
                }
                assertEquals(HttpStatusCode.TooManyRequests, rateLimitedResponse.status)

                // Different first IP should not be rate limited
                val allowedResponse = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Forwarded-For", "5.6.7.8, 1.2.3.4")
                }
                assertEquals(HttpStatusCode.Unauthorized, allowedResponse.status)
            }
        } finally {
            EnvUtil.overrides.remove(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS.name)
            EnvUtil.overrides.remove(EnvUtil.REAL_IP_HEADER.name)
        }
    }

    @Test
    fun testLoginRateLimit_XRealIp() {
        EnvUtil.addOverride(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS, "2")
        EnvUtil.addOverride(EnvUtil.REAL_IP_HEADER, "X-Real-IP")
        try {
            testApp(false) { client ->
                // Exhaust rate limit for IP 1.2.3.4 using X-Real-IP
                repeat(2) {
                    val response = client.post("/api/auth/login") {
                        parameter("username", "Admin")
                        parameter("password", "wrongpassword")
                        parameter("deviceId", "test")
                        parameter("deviceName", "Test Device")
                        header("X-Real-IP", "1.2.3.4")
                    }
                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }

                // Should be rate limited for 1.2.3.4
                val rateLimited = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Real-IP", "1.2.3.4")
                }
                assertEquals(HttpStatusCode.TooManyRequests, rateLimited.status)

                // X-Forwarded-For should be ignored
                val allowed = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Forwarded-For", "1.2.3.4")
                }
                assertEquals(HttpStatusCode.Unauthorized, allowed.status)
            }
        } finally {
            EnvUtil.overrides.remove(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS.name)
            EnvUtil.overrides.remove(EnvUtil.REAL_IP_HEADER.name)
        }
    }

    @Test
    fun testLoginRateLimit_NoRealIpHeader_UsesRemoteAddress() {
        EnvUtil.addOverride(EnvUtil.REAL_IP_HEADER, "")
        EnvUtil.addOverride(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS, "2")
        try {
            testApp(false) { client ->
                // Without REAL_IP_HEADER, the remote address is used
                // Even if X-Forwarded-For is set, it should be ignored
                repeat(2) {
                    val response = client.post("/api/auth/login") {
                        parameter("username", "Admin")
                        parameter("password", "wrongpassword")
                        parameter("deviceId", "test")
                        parameter("deviceName", "Test Device")
                        header("X-Forwarded-For", "1.2.3.4")
                    }
                    assertEquals(HttpStatusCode.Unauthorized, response.status)
                }

                // Should be rate limited (all requests come from same remote address)
                val rateLimitedResponse = client.post("/api/auth/login") {
                    parameter("username", "Admin")
                    parameter("password", "wrongpassword")
                    parameter("deviceId", "test")
                    parameter("deviceName", "Test Device")
                    header("X-Forwarded-For", "5.6.7.8")
                }
                assertEquals(HttpStatusCode.TooManyRequests, rateLimitedResponse.status)
            }
        } finally {
            EnvUtil.overrides.remove(EnvUtil.LOGIN_RATE_LIMIT_ATTEMPTS.name)
            EnvUtil.overrides.remove(EnvUtil.REAL_IP_HEADER.name)
        }
    }

    // endregion
}