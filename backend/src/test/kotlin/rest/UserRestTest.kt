package rest

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.dto.UserDto
import wtf.ndu.vibin.dto.UserEditDto
import wtf.ndu.vibin.repos.UserRepo
import kotlin.test.*

class UserRestTest {

    @Test
    fun testGetUsers() = testApp { client ->
        UserTestUtils.createTestUser("user1", "password1")
        UserTestUtils.createTestUser("user2", "password2")

        val response = client.get("/api/users")
        assertTrue(response.status.isSuccess())

        val users = response.body<List<UserDto>>()
        assertTrue(users.size == 3) // including default admin user
        assertTrue(users.any { it.username == "user1" })
        assertTrue(users.any { it.username == "user2" })
    }

    @Test
    fun testGetUserById() = testApp { client ->
        val testUser = UserTestUtils.createTestUser("singleuser", "password123")
        val response = client.get("/api/users/${testUser.id.value}")
        assertTrue(response.status.isSuccess())

        val user = response.body<UserDto>()
        assertEquals(user.username, "singleuser")
        assertEquals(user.displayName, "singleuser")
        assertEquals(user.email, null)
        assertEquals(user.isAdmin, false)
        assertEquals(user.isActive, true)
        assertEquals(user.profilePicture, null)
    }

    @Test
    fun testGetUserById_NotFound() = testApp { client ->
        val response = client.get("/api/users/999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testCreateUser() = testApp { client ->
        val response = client.post("/api/users") {
            setBody(UserEditDto(
                username = "testuser",
                displayName = "Test User",
                email = null,
                isAdmin = false,
                isActive = null,
                profilePictureUrl = null,
                password = "password123"
            ))
        }
        assertTrue(response.status.isSuccess())

        val users = UserRepo.getAllUsers()
        transaction {
            val user = users.find { it.username == "testuser" }
            assertNotNull(user)

            assertEquals(user.displayName, "Test User")
            assertNull(user.email)
            assertFalse(user.isAdmin)
            assertTrue(user.isActive)
            assertNull(user.profilePicture)
            assertContentEquals(user.passwordHash, CryptoUtil.hashPassword("password123", user.salt))
        }
    }

    @Test
    fun testEditUser() = testApp { client ->
        val testUser = UserTestUtils.createTestUser("edituser", "oldpassword")
        val response = client.put("/api/users/${testUser.id.value}") {
            setBody(UserEditDto(
                username = "editeduser",
                displayName = "Edited User",
                email = "edited.user@example.com",
                isAdmin = true,
                isActive = null,
                profilePictureUrl = null,
                password = "other password that won't be changed"
            ))
        }
        assertTrue(response.status.isSuccess())

        val user = UserRepo.getById(testUser.id.value)
        transaction {
            assertNotNull(user)

            assertEquals(user.username, "editeduser")
            assertEquals(user.displayName, "Edited User")
            assertEquals(user.email, "edited.user@example.com")
            assertTrue(user.isAdmin)
            assertEquals(testUser.isActive, user.isActive) // unchanged
            assertNull(user.profilePicture)
            assertContentEquals(user.passwordHash, CryptoUtil.hashPassword("oldpassword", user.salt)) // unchanged
        }
    }

    @Test
    fun testEditUser_NoChanges() = testApp { client ->
        val testUser = UserTestUtils.createTestUser("nochangeuser", "password123")
        val response = client.put("/api/users/${testUser.id.value}") {
            setBody(
                UserEditDto(
                    username = null,
                    displayName = null,
                    email = null,
                    isAdmin = null,
                    isActive = null,
                    profilePictureUrl = null,
                    password = null
                )
            )
        }
        assertTrue(response.status.isSuccess())

        val user = UserRepo.getById(testUser.id.value)
        transaction {
            assertNotNull(user)

            assertEquals(testUser.username, user.username) // unchanged
            assertEquals(testUser.displayName, user.displayName) // unchanged
            assertEquals(testUser.email, user.email) // unchanged
            assertEquals(testUser.isAdmin, user.isAdmin) // unchanged
            assertEquals(testUser.isActive, user.isActive) // unchanged
            assertEquals(testUser.profilePicture, user.profilePicture) // unchanged
            assertContentEquals(testUser.salt, user.salt) // unchanged
            assertContentEquals(testUser.passwordHash, user.passwordHash) // unchanged
        }
    }

    @Test
    fun testDeleteUser() = testApp { client ->
        val testUser = UserTestUtils.createTestUser("deleteuser", "password123")
        assertEquals(2, UserRepo.count())

        val response = client.delete("/api/users/${testUser.id.value}")
        assertTrue(response.status.isSuccess())

        val user = UserRepo.getById(testUser.id.value)
        assertNull(user)
        assertEquals(1, UserRepo.count())
    }
}