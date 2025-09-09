package rest

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.auth.CryptoUtil
import wtf.ndu.vibin.dto.users.UserDto
import wtf.ndu.vibin.dto.users.UserEditDto
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
    fun testGetUsers_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserWithSession(
            "noperms",
            "password"
        ) // No VIEW_USERS permission
        val response = client.get("/api/users") {
            header("Authorization", "Bearer $token")
        }
        assertEquals(403, response.status.value)
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
    fun testGetUserById_NoPermission() = testApp(false) { client ->
        val testUser = UserTestUtils.createTestUser("nopermsuser", "password123")
        val (_, token) = UserTestUtils.createUserWithSession(
            "noperms",
            "password"
        ) // No VIEW_USERS permission

        val response = client.get("/api/users/${testUser.id.value}") {
            bearerAuth(token)
        }
        assertEquals(403, response.status.value)
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
                oldPassword = null,
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
    fun testCreateUser_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserWithSession(
            "noperms",
            "password"
        ) // No MANAGE_USERS permission
        val response = client.post("/api/users") {
            header("Authorization", "Bearer $token")
            setBody(UserEditDto(
                username = "testuser",
                displayName = "Test User",
                email = null,
                isAdmin = false,
                isActive = null,
                profilePictureUrl = null,
                password = "password123",
                oldPassword = null
            ))
        }
        assertEquals(403, response.status.value)

        assertEquals(2, UserRepo.count()) // only default admin user and noperms user
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
                password = "newpassword",
                oldPassword = "oldpassword"
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
            assertContentEquals(user.passwordHash, CryptoUtil.hashPassword("newpassword", user.salt))
        }
    }

    @Test
    fun testChangePassword_IncorrectOldPassword() = testApp(false) { client ->
        val (testUser, token) = UserTestUtils.createUserWithSession("changepassuser", "oldpassword")
        val response = client.put("/api/users/${testUser.id.value}") {
            bearerAuth(token)
            setBody(
                UserEditDto(
                    username = null,
                    displayName = null,
                    email = null,
                    isAdmin = null,
                    isActive = null,
                    profilePictureUrl = null,
                    password = "newpassword",
                    oldPassword = "wrongoldpassword"
                )
            )
        }
        assertEquals(403, response.status.value)
    }

    @Test
    fun testChangedPassword_NoOldPasswordAsAdmin() = testApp { client ->
        val testUser = UserTestUtils.createTestUser("adminchangepassuser", "oldpassword")

        val response = client.put("/api/users/${testUser.id.value}") {
            setBody(
                UserEditDto(
                    username = null,
                    displayName = null,
                    email = null,
                    isAdmin = null,
                    isActive = null,
                    profilePictureUrl = null,
                    password = "newpassword",
                    oldPassword = null // no old password provided
                )
            )
        }
        assertTrue(response.status.isSuccess())

        val user = UserRepo.getById(testUser.id.value)
        transaction {
            assertNotNull(user)
            assertContentEquals(user.passwordHash, CryptoUtil.hashPassword("newpassword", user.salt))
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
                    password = null,
                    oldPassword = null
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
    fun testEditUser_NoPermission() = testApp(false) { client ->
        val testUser = UserTestUtils.createTestUser("nopermsedit", "password123")
        val (_, token) = UserTestUtils.createUserWithSession(
            "noperms",
            "password"
        ) // No MANAGE_USERS permission
        val response = client.put("/api/users/${testUser.id.value}") {
            bearerAuth(token)
            setBody(UserEditDto(
                username = "editeduser",
                displayName = "Edited User",
                email = "edited@example.cokm",
                isAdmin = true,
                isActive = null,
                profilePictureUrl = null,
                password = "other password that won't be changed",
                oldPassword = null
            ))
        }
        assertEquals(403, response.status.value)

        val user = UserRepo.getById(testUser.id.value)
        transaction {
            assertNotNull(user)

            assertEquals(user.username, "nopermsedit")
            assertNull(user.displayName)
            assertNull(user.email)
            assertFalse(user.isAdmin)
            assertTrue(user.isActive)
            assertNull(user.profilePicture)
            assertContentEquals(user.passwordHash, CryptoUtil.hashPassword("password123", user.salt))
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

    @Test
    fun testDeleteUser_NoPermission() = testApp(false) { client ->
        val testUser = UserTestUtils.createTestUser("nopermsdelete", "password123")
        val (_, token) = UserTestUtils.createUserWithSession(
            "noperms",
            "password"
        ) // No DELETE_USERS permission
        val response = client.delete("/api/users/${testUser.id.value}") {
            bearerAuth(token)
        }
        assertEquals(403, response.status.value)

        val user = UserRepo.getById(testUser.id.value)
        assertNotNull(user)
        assertEquals(3, UserRepo.count()) // default admin, noperms user, nopermsdelete user
    }
}