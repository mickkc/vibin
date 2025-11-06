package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PermissionRepo
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionRestTest {

    @Serializable
    private data class PermissionToggleResponse(
        val granted: Boolean
    )

    // region Get Own Permissions

    @Test
    fun testGetOwnPermissions() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "password",
            PermissionType.VIEW_TRACKS to true,
            PermissionType.VIEW_ALBUMS to true,
            PermissionType.UPLOAD_TRACKS to false
        )

        val response = client.get("/api/permissions") {
            bearerAuth(token)
        }

        assertTrue(response.status.isSuccess())

        val permissions = response.body<List<String>>()

        assertTrue(permissions.contains(PermissionType.VIEW_TRACKS.id))
        assertTrue(permissions.contains(PermissionType.VIEW_ALBUMS.id))
        assertFalse(permissions.contains(PermissionType.UPLOAD_TRACKS.id))
    }

    @Test
    fun testGetOwnPermissions_AdminHasAllPermissions() = testApp { client ->

        val response = client.get("/api/permissions")

        assertTrue(response.status.isSuccess())

        val permissions = response.body<List<String>>()

        // Admin should have all permissions
        assertEquals(PermissionType.entries.size, permissions.size)
        PermissionType.entries.forEach { permissionType ->
            assertTrue(permissions.contains(permissionType.id))
        }
    }

    @Test
    fun testGetOwnPermissions_Unauthorized() = testApp(false) { client ->
        val response = client.get("/api/permissions")

        assertEquals(401, response.status.value)
    }

    // endregion

    // region Get User Permissions

    @Test
    fun testGetUserPermissions() = testApp { client ->
        val (targetUser, _) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "targetuser",
            password = "password",
            PermissionType.VIEW_TRACKS to true,
            PermissionType.MANAGE_TRACKS to true,
            PermissionType.DELETE_TRACKS to false
        )

        val response = client.get("/api/permissions/${targetUser.id.value}")

        assertTrue(response.status.isSuccess())

        val permissions = response.body<List<String>>()

        assertContains(permissions, PermissionType.VIEW_TRACKS.id)
        assertContains(permissions, PermissionType.MANAGE_TRACKS.id)
        assertFalse(permissions.contains(PermissionType.DELETE_TRACKS.id))
    }

    @Test
    fun testGetUserPermissions_NotFound() = testApp { client ->

        val response = client.get("/api/permissions/9999")
        assertEquals(404, response.status.value)
    }

    @Test
    fun testGetUserPermissions_NoPermission() = testApp(false) { client ->
        val (targetUser, _) = UserTestUtils.createUserAndSessionWithPermissions("targetuser", "password")

        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "regularuser",
            password = "password",
            PermissionType.MANAGE_PERMISSIONS to false
        )

        val response = client.get("/api/permissions/${targetUser.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Toggle Permission

    @Test
    fun testTogglePermission_GrantPermission() = testApp { client ->
        val (targetUser, _) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "targetuser",
            password = "password",
            PermissionType.MANAGE_TRACKS to false
        )

        // Initially user doesn't have MANAGE_TRACKS permission
        assertFalse(PermissionRepo.hasPermission(targetUser.id.value, PermissionType.MANAGE_TRACKS))

        val response = client.put("/api/permissions/${targetUser.id.value}") {
            parameter("permissionId", PermissionType.MANAGE_TRACKS.id)
        }

        assertTrue(response.status.isSuccess())

        val toggleResponse = response.body<PermissionToggleResponse>()
        assertTrue(toggleResponse.granted)

        // Verify permission was granted
        assertTrue(PermissionRepo.hasPermission(targetUser.id.value, PermissionType.MANAGE_TRACKS))
    }

    @Test
    fun testTogglePermission_RevokePermission() = testApp { client ->
        val (targetUser, _) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "targetuser",
            password = "password",
            PermissionType.MANAGE_TRACKS to true
        )

        // Initially user has MANAGE_TRACKS permission
        assertTrue(PermissionRepo.hasPermission(targetUser.id.value, PermissionType.MANAGE_TRACKS))

        val response = client.put("/api/permissions/${targetUser.id.value}") {
            parameter("permissionId", PermissionType.MANAGE_TRACKS.id)
        }

        assertTrue(response.status.isSuccess())

        val toggleResponse = response.body<PermissionToggleResponse>()
        assertFalse(toggleResponse.granted)

        // Verify permission was revoked
        assertFalse(PermissionRepo.hasPermission(targetUser.id.value, PermissionType.MANAGE_TRACKS))
    }

    @Test
    fun testTogglePermission_InvalidPermissionId() = testApp { client ->
        val targetUser = UserTestUtils.createTestUser("targetuser", "password")

        val response = client.put("/api/permissions/${targetUser.id.value}") {
            parameter("permissionId", "invalid_permission_id")
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testTogglePermission_UserNotFound() = testApp { client ->

        val response = client.put("/api/permissions/9999") {
            parameter("permissionId", PermissionType.MANAGE_TRACKS.id)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testTogglePermission_NoPermission() = testApp(false) { client ->
        val targetUser = UserTestUtils.createTestUser("targetuser", "password")

        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "regularuser",
            password = "password",
            PermissionType.MANAGE_PERMISSIONS to false
        )

        val response = client.put("/api/permissions/${targetUser.id.value}") {
            parameter("permissionId", PermissionType.MANAGE_TRACKS.id)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testTogglePermission_MissingPermissionIdParameter() = testApp { client ->
        val targetUser = UserTestUtils.createTestUser("targetuser", "password")

        val response = client.put("/api/permissions/${targetUser.id.value}")

        assertEquals(400, response.status.value)
    }

    // endregion
}
