package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import utils.testApp
import wtf.ndu.vibin.dto.LoginResultDto
import wtf.ndu.vibin.repos.SessionRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthRestTest {

    @Test
    fun testLogin() = testApp { client ->
        val response = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
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
        }
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.Unauthorized, response.status)
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
    fun validateSession_Failure() = testApp { client ->

        // Logout first to invalidate the session
        client.post("/api/auth/logout")

        val response = client.post("/api/auth/validate")
        assertFalse(response.status.isSuccess())
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun changePassword() = testApp { client ->

        assertEquals(1, SessionRepo.count()) // one active session (default admin user)

        val response = client.post("/api/auth/password") {
            parameter("currentPassword", "admin")
            parameter("newPassword", "newpassword123")
        }
        assertTrue(response.status.isSuccess())

        val responseBody = response.body<Map<String, Boolean>>()
        assertTrue(responseBody["success"] == true)

        // Password changed, session should be invalidated
        assertEquals(0, SessionRepo.count())

        // Try to login with old password (should fail)
        val oldLoginResponse = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "admin")
        }
        assertFalse(oldLoginResponse.status.isSuccess())
        assertEquals(HttpStatusCode.Unauthorized, oldLoginResponse.status)

        // Try to login with new password (should succeed)
        val newLoginResponse = client.post("/api/auth/login") {
            parameter("username", "Admin")
            parameter("password", "newpassword123")
        }
        assertTrue(newLoginResponse.status.isSuccess())

        assertEquals(1, SessionRepo.count())
    }
}