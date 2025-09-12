package rest

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import utils.PlaylistTestUtils
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.playlists.PlaylistDto
import wtf.ndu.vibin.dto.playlists.PlaylistEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.PermissionRepo
import wtf.ndu.vibin.repos.PlaylistRepo
import wtf.ndu.vibin.repos.SessionRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistRestTest {

    // region View

    @Test
    fun testViewOwnPlaylists() = testApp { client ->
        PlaylistTestUtils.createPlaylist("Another Playlist", false, 1)
        PlaylistTestUtils.createPlaylist("My Public Playlist", true, 1)
        PlaylistTestUtils.createPlaylist("My Private Playlist", false, 1)
        assertEquals(3, PlaylistRepo.count(1))

        val result = client.get("/api/playlists") {
            parameter("pageSize", 2)
        }

        assertTrue(result.status.isSuccess())

        val fetchedPlaylists = result.body<PaginatedDto<PlaylistDto>>()
        assertEquals(3, fetchedPlaylists.total)
        assertEquals(2, fetchedPlaylists.items.size)

        val publicPlaylist = fetchedPlaylists.items.find { it.name == "My Public Playlist" }
        assertNotNull(publicPlaylist)
        assertEquals("My Public Playlist", publicPlaylist.name)
        assertEquals("Test Description", publicPlaylist.description)
        assertTrue(publicPlaylist.public)
        assertEquals(0, publicPlaylist.collaborators.size)
        assertEquals(1, publicPlaylist.owner.id)

        val privatePlaylist = fetchedPlaylists.items.find { it.name == "My Private Playlist" }
        assertNotNull(privatePlaylist)
        assertEquals("My Private Playlist", privatePlaylist.name)
        assertEquals("Test Description", privatePlaylist.description)
        assertFalse(privatePlaylist.public)
        assertEquals(0, privatePlaylist.collaborators.size)
        assertEquals(1, privatePlaylist.owner.id)
    }

    @Test
    fun testViewPlaylistAsCollaborator() = testApp(false) { client ->

        val (collaborator, token) = UserTestUtils.createUserWithSession("collabview", "password")
        PlaylistTestUtils.createPlaylist("Collab Playlist", false, 1, collaborator.id.value)
        assertEquals(1, PlaylistRepo.count(collaborator.id.value))

        val result = client.get("/api/playlists") {
            bearerAuth(token)
        }

        assertTrue(result.status.isSuccess())

        val fetchedPlaylists = result.body<PaginatedDto<PlaylistDto>>()
        assertEquals(1, fetchedPlaylists.total)
        assertEquals(1, fetchedPlaylists.items.size)

        val fetchedPlaylist = fetchedPlaylists.items[0]
        assertEquals("Collab Playlist", fetchedPlaylist.name)
        assertEquals("Test Description", fetchedPlaylist.description)
        assertEquals(false, fetchedPlaylist.public)
        assertEquals(1, fetchedPlaylist.collaborators.size)
        assertEquals(collaborator.id.value, fetchedPlaylist.collaborators[0].id)
        assertEquals(1, fetchedPlaylist.owner.id)
    }

    @Test
    fun testViewPlaylistAsNonCollaborator() = testApp(false) { client ->
        val (nonCollaborator, token) = UserTestUtils.createUserWithSession("noncollab", "password")

        PlaylistTestUtils.createPlaylist("Private Playlist", false, 1)
        assertEquals(0, PlaylistRepo.count(nonCollaborator.id.value))

        val result = client.get("/api/playlists") {
            bearerAuth(token)
        }

        assertTrue(result.status.isSuccess())

        val fetchedPlaylists = result.body<PaginatedDto<PlaylistDto>>()
        assertEquals(0, fetchedPlaylists.total)
        assertEquals(0, fetchedPlaylists.items.size)
    }
    // endregion

    // region Create

    @Test
    fun testCreatePlaylist() = testApp { client ->

        val collaborator = UserTestUtils.createTestUser("collab", "password")

        val result = client.post("/api/playlists") {
            setBody(PlaylistEditDto(
                name = "My Playlist",
                description = "A cool playlist",
                isPublic = true,
                collaboratorIds = listOf(collaborator.id.value)
            ))
        }

        assertTrue(result.status.isSuccess())

        val playlist = result.body<PlaylistDto>()
        assertEquals("My Playlist", playlist.name)
        assertEquals("A cool playlist", playlist.description)
        assertEquals(true, playlist.public)
        assertEquals(1, playlist.collaborators.size)

        val collab = playlist.collaborators[0]
        assertEquals(collaborator.id.value, collab.id)
        assertEquals(collaborator.username, collab.username)

        assertEquals(1, playlist.owner.id)

        val dbPlaylist = PlaylistRepo.getById(1, 1)

        assertNotNull(dbPlaylist)
        assertEquals(1, dbPlaylist.id.value)
        assertEquals("My Playlist", dbPlaylist.name)
        assertEquals("A cool playlist", dbPlaylist.description)
        assertEquals(true, dbPlaylist.public)
        assertTrue(PlaylistRepo.checkOwnership(dbPlaylist, 1))
    }

    @Test
    fun testCreatePlaylist_NoName() = testApp { client ->

        val result = client.post("/api/playlists") {
            setBody(PlaylistEditDto(
                name = "",
                description = "A cool playlist",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(400, result.status.value)
        assertEquals(0, PlaylistRepo.count(1))
    }

    @Test
    fun testCreatePlaylist_NoPermission() = testApp(false) { client ->
        val (user, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms3", "password",
            PermissionType.CREATE_PUBLIC_PLAYLISTS to false,
            PermissionType.CREATE_PRIVATE_PLAYLISTS to false
        )

        val publicResult = client.post("/api/playlists") {
            bearerAuth(token)
            setBody(PlaylistEditDto(
                name = "My Playlist",
                description = "A cool playlist",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(403, publicResult.status.value)

        val privateResult = client.post("/api/playlists") {
            bearerAuth(token)
            setBody(PlaylistEditDto(
                name = "My Playlist",
                description = "A cool playlist",
                isPublic = false,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(403, privateResult.status.value)

        assertEquals(0, PlaylistRepo.count(user.id.value))
    }
    // endregion

    // region Edit

    @Test
    fun testUpdatePlaylist() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("Old Name", false, 1)
        assertEquals(1, PlaylistRepo.count(1))

        val collaborator = UserTestUtils.createTestUser("collab2", "password")

        val result = client.put("/api/playlists/${playlist.id.value}") {
            setBody(PlaylistEditDto(
                name = "New Name",
                description = "Updated Description",
                isPublic = true,
                collaboratorIds = listOf(collaborator.id.value)
            ))
        }

        assertTrue(result.status.isSuccess())

        val updatedPlaylist = result.body<PlaylistDto>()

        assertEquals("New Name", updatedPlaylist.name)
        assertEquals("Updated Description", updatedPlaylist.description)
        assertEquals(true, updatedPlaylist.public)
        assertEquals(1, updatedPlaylist.collaborators.size)
        assertEquals(collaborator.id.value, updatedPlaylist.collaborators[0].id)
        assertEquals(1, updatedPlaylist.owner.id)

        assertEquals(1, PlaylistRepo.count(1))

        val dbPlaylist = PlaylistRepo.getById(playlist.id.value, 1)
        assertNotNull(dbPlaylist)

        assertEquals("New Name", dbPlaylist.name)
        assertEquals("Updated Description", dbPlaylist.description)
        assertEquals(true, dbPlaylist.public)
        assertTrue(PlaylistRepo.checkOwnership(dbPlaylist, 1))
    }

    @Test
    fun testEditPlaylist_Collaborator_NoPermission() = testApp(false) { client ->
        val collaborator = UserTestUtils.createTestUser("collab3", "password")

        val playlist = PlaylistTestUtils.createPlaylist("Old Name", false, 1, collaborator.id.value)
        assertEquals(1, PlaylistRepo.count(1))

        SessionRepo.addSession(collaborator, "collab3-session-token")

        val result = client.put("/api/playlists/${playlist.id.value}") {
            bearerAuth("collab3-session-token")
            setBody(PlaylistEditDto(
                name = "New Name",
                description = "Updated Description",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(404, result.status.value)

        val dbPlaylist = PlaylistRepo.getById(playlist.id.value, 1)
        assertNotNull(dbPlaylist)

        assertEquals("Old Name", dbPlaylist.name)
        assertEquals("Test Description", dbPlaylist.description) // unchanged
        assertEquals(false, dbPlaylist.public) // unchanged
        assertTrue(PlaylistRepo.checkOwnership(dbPlaylist, 1))
    }

    @Test
    fun testEditPlaylist_Collaborator_WithPermission() = testApp(false) { client ->
        val collaborator = UserTestUtils.createTestUser("collab4", "password")

        val playlist = PlaylistTestUtils.createPlaylist("Old Name", false, 1, collaborator.id.value)
        assertEquals(1, PlaylistRepo.count(1))

        PermissionRepo.addPermission(collaborator.id.value, PermissionType.EDIT_COLLABORATIVE_PLAYLISTS)
        SessionRepo.addSession(collaborator, "collab4-session-token")

        val result = client.put("/api/playlists/${playlist.id.value}") {
            bearerAuth("collab4-session-token")
            setBody(PlaylistEditDto(
                name = "New Name",
                description = "Updated Description",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertTrue(result.status.isSuccess())

        val updatedPlaylist = result.body<PlaylistDto>()
        assertEquals("New Name", updatedPlaylist.name)
        assertEquals("Updated Description", updatedPlaylist.description)
        assertEquals(true, updatedPlaylist.public)
        assertEquals(0, updatedPlaylist.collaborators.size)
    }

    @Test
    fun testEditPlaylist_NoName() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("Old Name", false, 1)
        assertEquals(1, PlaylistRepo.count(1))

        val result = client.put("/api/playlists/${playlist.id.value}") {
            setBody(PlaylistEditDto(
                name = "",
                description = "Updated Description",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(400, result.status.value)

        val dbPlaylist = PlaylistRepo.getById(playlist.id.value, 1)
        assertNotNull(dbPlaylist)

        assertEquals("Old Name", dbPlaylist.name)
        assertEquals("Test Description", dbPlaylist.description) // unchanged
        assertEquals(false, dbPlaylist.public) // unchanged
        assertTrue(PlaylistRepo.checkOwnership(dbPlaylist, 1))
    }

    @Test
    fun testEditPlaylist_NoPermission() = testApp(false) { client ->

        val (user, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms2", "password",
            PermissionType.MANAGE_PLAYLISTS to false
        )

        val playlist = PlaylistTestUtils.createPlaylist("Old Name", false, user.id.value)
        assertEquals(1, PlaylistRepo.count(user.id.value))

        val result = client.put("/api/playlists/${playlist.id.value}") {
            bearerAuth(token)
            setBody(PlaylistEditDto(
                name = "New Name",
                description = "Updated Description",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(403, result.status.value)

        val dbPlaylist = PlaylistRepo.getById(playlist.id.value, user.id.value)
        assertNotNull(dbPlaylist)

        assertEquals("Old Name", dbPlaylist.name)
        assertEquals("Test Description", dbPlaylist.description) // unchanged
        assertEquals(false, dbPlaylist.public) // unchanged
        assertTrue(PlaylistRepo.checkOwnership(dbPlaylist, user.id.value))
    }

    @Test
    fun testEditPlaylist_InvalidType() = testApp(false) { client ->

        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms", "password",
            PermissionType.CREATE_PUBLIC_PLAYLISTS to false,
            PermissionType.CREATE_PRIVATE_PLAYLISTS to false)

        val playlist = PlaylistTestUtils.createPlaylist("Old Name", false, 1)
        assertEquals(1, PlaylistRepo.count(1))

        val privateRresult = client.put("/api/playlists/${playlist.id.value}") {
            bearerAuth(token)
            setBody(PlaylistEditDto(
                name = "New Name",
                description = "Updated Description",
                isPublic = false,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(403, privateRresult.status.value)

        val publicResult = client.put("/api/playlists/${playlist.id.value}") {
            bearerAuth(token)
            setBody(PlaylistEditDto(
                name = "New Name",
                description = "Updated Description",
                isPublic = true,
                collaboratorIds = emptyList()
            ))
        }

        assertEquals(403, publicResult.status.value)

        val dbPlaylist = PlaylistRepo.getById(playlist.id.value, 1)
        assertNotNull(dbPlaylist)

        assertEquals("Old Name", dbPlaylist.name) // unchanged
        assertEquals("Test Description", dbPlaylist.description) // unchanged
        assertFalse(dbPlaylist.public) // unchanged
    }
    // endregion

    // region Delete

    @Test
    fun testDeletePlaylist() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("To Be Deleted", false, 1)
        assertEquals(1, PlaylistRepo.count(1))

        val result = client.delete("/api/playlists/${playlist.id.value}")

        assertTrue(result.status.isSuccess())

        assertEquals(0, PlaylistRepo.count(1))
        val dbPlaylist = PlaylistRepo.getById(playlist.id.value, 1)
        assertNull(dbPlaylist)
    }

    @Test
    fun testDeletePlaylist_NotOwner() = testApp(false) { client ->
        val nonOwner = UserTestUtils.createTestUser("nonowner", "password")
        val playlist = PlaylistTestUtils.createPlaylist("Not My Playlist", false, 1)
        assertEquals(1, PlaylistRepo.count(1))

        SessionRepo.addSession(nonOwner, "nonowner-session-token")
        val result = client.delete("/api/playlists/${playlist.id.value}") {
            bearerAuth("nonowner-session-token")
        }

        assertEquals(404, result.status.value)
        assertEquals(1, PlaylistRepo.count(1))
    }

    @Test
    fun testDeletePlaylist_Collaborator_NoPermission() = testApp(false) { client ->
        val collaborator = UserTestUtils.createTestUser("collab5", "password")
        val playlist = PlaylistTestUtils.createPlaylist("Collab Playlist", false, 1, collaborator.id.value)
        assertEquals(1, PlaylistRepo.count(1))

        SessionRepo.addSession(collaborator, "collab5-session-token")
        val result = client.delete("/api/playlists/${playlist.id.value}") {
            bearerAuth("collab5-session-token")
        }

        assertEquals(404, result.status.value)
        assertEquals(1, PlaylistRepo.count(1))
    }

    @Test
    fun testDeletePlaylist_Collaborator_WithPermission() = testApp(false) { client ->
        val collaborator = UserTestUtils.createTestUser("collab6", "password")
        val playlist = PlaylistTestUtils.createPlaylist("Collab Playlist", false, 1, collaborator.id.value)
        assertEquals(1, PlaylistRepo.count(1))

        PermissionRepo.addPermission(collaborator.id.value, PermissionType.DELETE_COLLABORATIVE_PLAYLISTS)
        SessionRepo.addSession(collaborator, "collab6-session-token")
        val result = client.delete("/api/playlists/${playlist.id.value}") {
            bearerAuth("collab6-session-token")
        }

        assertTrue(result.status.isSuccess())
        assertEquals(0, PlaylistRepo.count(1))
    }

    @Test
    fun testDeletePlaylist_NoPermission() = testApp(false) { client ->
        val (user, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms5", "password",
            PermissionType.DELETE_OWN_PLAYLISTS to false
        )

        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", false, user.id.value)
        assertEquals(1, PlaylistRepo.count(user.id.value))

        val result = client.delete("/api/playlists/${playlist.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, result.status.value)
        assertEquals(1, PlaylistRepo.count(user.id.value))
    }
    // endregion
}