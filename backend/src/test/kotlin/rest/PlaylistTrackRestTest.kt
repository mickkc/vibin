package rest

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.isSuccess
import utils.PlaylistTestUtils
import utils.TrackTestUtils
import utils.UserTestUtils
import utils.testApp
import de.mickkc.vibin.dto.playlists.PlaylistDto
import de.mickkc.vibin.dto.playlists.PlaylistTrackDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.PlaylistTrackRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistTrackRestTest {

    // region Add Track to Playlist

    @Test
    fun testAddTrackToPlaylist() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
        }

        assertTrue(response.status.isSuccess())

        val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
        assertEquals(1, playlistTracks.size)
        assertEquals(track.id.value, playlistTracks[0].track.id)
        assertEquals(0, playlistTracks[0].position)
    }

    @Test
    fun testAddTrackToPlaylist_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_PLAYLIST_TRACKS to false
        )

        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testAddTrackToPlaylist_PlaylistNotFound() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.post("/api/playlists/9999/tracks") {
            parameter("trackId", track.id.value)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testAddTrackToPlaylist_TrackNotFound() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", 9999)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testAddTrackToPlaylist_MissingTrackId() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.post("/api/playlists/${playlist.id.value}/tracks")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testAddTrackToPlaylist_NotOwnerOrCollaborator() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserWithSession("noncollab", "password")

        val playlist = PlaylistTestUtils.createPlaylist("Private Playlist", false, 1)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testAddTrackToPlaylist_AsCollaborator() = testApp(false) { client ->
        val (collaborator, token) = UserTestUtils.createUserWithSession("collab", "password")

        val playlist = PlaylistTestUtils.createPlaylist("Collab Playlist", false, 1, collaborator.id.value)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token)
        }

        assertTrue(response.status.isSuccess())

        val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
        assertEquals(1, playlistTracks.size)
        assertEquals(track.id.value, playlistTracks[0].track.id)
    }

    @Test
    fun testAddMultipleTracksToPlaylist() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")
        val track3 = TrackTestUtils.createTrack("Track 3", "Album", "Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track2.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track3.id.value)
        }

        val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
        assertEquals(3, playlistTracks.size)
        assertEquals(track1.id.value, playlistTracks[0].track.id)
        assertEquals(0, playlistTracks[0].position)
        assertEquals(track2.id.value, playlistTracks[1].track.id)
        assertEquals(1, playlistTracks[1].position)
        assertEquals(track3.id.value, playlistTracks[2].track.id)
        assertEquals(2, playlistTracks[2].position)
    }

    // endregion

    // region Remove Track from Playlist

    @Test
    fun testRemoveTrackFromPlaylist() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track2.id.value)
        }

        // Remove first track
        val response = client.delete("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }

        assertTrue(response.status.isSuccess())

        val playlistTracks = PlaylistTrackRepo.getTracksAsDtos(playlist)
        assertEquals(1, playlistTracks.size)
        assertEquals(track2.id.value, playlistTracks[0].track.id)
        assertEquals(0, playlistTracks[0].position)
    }

    @Test
    fun testRemoveTrackFromPlaylist_NoPermission() = testApp(false) { client ->
        val (user, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_PLAYLIST_TRACKS to false
        )

        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1, user.id.value)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
        }

        val response = client.delete("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testRemoveTrackFromPlaylist_PlaylistNotFound() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.delete("/api/playlists/9999/tracks") {
            parameter("trackId", track.id.value)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testRemoveTrackFromPlaylist_TrackNotFound() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.delete("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", 9999)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testRemoveTrackFromPlaylist_MissingTrackId() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.delete("/api/playlists/${playlist.id.value}/tracks")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testRemoveTrackFromPlaylist_NotOwnerOrCollaborator() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserWithSession("noncollab", "password")

        val playlist = PlaylistTestUtils.createPlaylist("Private Playlist", false, 1)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
        }

        val response = client.delete("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token)
        }

        assertEquals(404, response.status.value) // Should not find playlist
    }

    // endregion

    // region Reorder Track in Playlist

    @Test
    fun testReorderTrackToBeginning() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")
        val track3 = TrackTestUtils.createTrack("Track 3", "Album", "Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track2.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track3.id.value)
        }

        // Move track3 to beginning (no afterTrackId)
        val response = client.put("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track3.id.value)
        }

        assertTrue(response.status.isSuccess())

        val playlistTracks = response.body<List<PlaylistTrackDto>>()
        assertEquals(3, playlistTracks.size)
        assertEquals(track3.id.value, playlistTracks[0].track.id)
        assertEquals(0, playlistTracks[0].position)
        assertEquals(track1.id.value, playlistTracks[1].track.id)
        assertEquals(1, playlistTracks[1].position)
        assertEquals(track2.id.value, playlistTracks[2].track.id)
        assertEquals(2, playlistTracks[2].position)
    }

    @Test
    fun testReorderTrack_Down() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")
        val track3 = TrackTestUtils.createTrack("Track 3", "Album", "Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track2.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track3.id.value)
        }

        // Move track1 after track2
        val response = client.put("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
            parameter("afterTrackId", track2.id.value)
        }

        assertTrue(response.status.isSuccess())

        val playlistTracks = response.body<List<PlaylistTrackDto>>()
        assertEquals(3, playlistTracks.size)
        assertEquals(track2.id.value, playlistTracks[0].track.id)
        assertEquals(0, playlistTracks[0].position)
        assertEquals(track1.id.value, playlistTracks[1].track.id)
        assertEquals(1, playlistTracks[1].position)
        assertEquals(track3.id.value, playlistTracks[2].track.id)
        assertEquals(2, playlistTracks[2].position)
    }

    @Test
    fun testReorderTrack_Up() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")
        val track3 = TrackTestUtils.createTrack("Track 3", "Album", "Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track2.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track3.id.value)
        }

        // Move track3 after track1
        val response = client.put("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track3.id.value)
            parameter("afterTrackId", track1.id.value)
        }

        assertTrue(response.status.isSuccess())

        val playlistTracks = response.body<List<PlaylistTrackDto>>()
        assertEquals(3, playlistTracks.size)
        assertEquals(track1.id.value, playlistTracks[0].track.id)
        assertEquals(0, playlistTracks[0].position)
        assertEquals(track3.id.value, playlistTracks[1].track.id)
        assertEquals(1, playlistTracks[1].position)
        assertEquals(track2.id.value, playlistTracks[2].track.id)
        assertEquals(2, playlistTracks[2].position)
    }

    @Test
    fun testReorderTrack_NoPermission() = testApp(false) { client ->
        val (user, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_PLAYLIST_TRACKS to false
        )

        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1, user.id.value)
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
        }
        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track2.id.value)
        }

        val response = client.put("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track1.id.value)
            parameter("afterTrackId", track2.id.value)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testReorderTrack_PlaylistNotFound() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.put("/api/playlists/9999/tracks") {
            parameter("trackId", track.id.value)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testReorderTrack_TrackNotFound() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.put("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", 9999)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testReorderTrack_AfterTrackNotFound() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        client.post("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
        }

        val response = client.put("/api/playlists/${playlist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            parameter("afterTrackId", 9999)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testReorderTrack_MissingTrackId() = testApp { client ->
        val playlist = PlaylistTestUtils.createPlaylist("My Playlist", true, 1)

        val response = client.put("/api/playlists/${playlist.id.value}/tracks")

        assertEquals(400, response.status.value)
    }

    // endregion

    // region Get Playlists Containing Track

    @Test
    fun testGetPlaylistsContainingTrack() = testApp { client ->
        val playlist1 = PlaylistTestUtils.createPlaylist("Playlist 1", true, 1)
        val playlist2 = PlaylistTestUtils.createPlaylist("Playlist 2", false, 1)
        PlaylistTestUtils.createPlaylist("Playlist 3", true, 1)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        // Add track to playlist1 and playlist2
        client.post("/api/playlists/${playlist1.id.value}/tracks") {
            parameter("trackId", track.id.value)
        }
        client.post("/api/playlists/${playlist2.id.value}/tracks") {
            parameter("trackId", track.id.value)
        }

        val response = client.get("/api/playlists/containing/${track.id.value}")

        assertTrue(response.status.isSuccess())

        val playlists = response.body<List<PlaylistDto>>()
        assertEquals(2, playlists.size)

        val playlistNames = playlists.map { it.name }
        assertTrue(playlistNames.contains("Playlist 1"))
        assertTrue(playlistNames.contains("Playlist 2"))
        assertFalse(playlistNames.contains("Playlist 3"))
    }

    @Test
    fun testGetPlaylistsContainingTrack_AsCollaborator() = testApp(false) { client ->
        val (owner, ownerToken) = UserTestUtils.createUserWithSession("owner", "password")
        val (collaborator, collabToken) = UserTestUtils.createUserWithSession("collab", "password")

        val playlist1 = PlaylistTestUtils.createPlaylist("Owner Playlist", false, owner.id.value)
        val playlist2 = PlaylistTestUtils.createPlaylist("Collab Playlist", false, owner.id.value, collaborator.id.value)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        // Add track to both playlists
        client.post("/api/playlists/${playlist1.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(ownerToken)
        }
        client.post("/api/playlists/${playlist2.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(ownerToken)
        }

        // Collaborator should only see the playlist they collaborate on
        val response = client.get("/api/playlists/containing/${track.id.value}") {
            bearerAuth(collabToken)
        }

        assertTrue(response.status.isSuccess())

        val playlists = response.body<List<PlaylistDto>>()
        assertEquals(1, playlists.size)
        assertEquals("Collab Playlist", playlists[0].name)
    }

    @Test
    fun testGetPlaylistsContainingTrack_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_PLAYLISTS to false
        )

        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.get("/api/playlists/containing/${track.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetPlaylistsContainingTrack_TrackNotFound() = testApp { client ->
        val response = client.get("/api/playlists/containing/9999")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testGetPlaylistsContainingTrack_NoPlaylists() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        val response = client.get("/api/playlists/containing/${track.id.value}")

        assertTrue(response.status.isSuccess())

        val playlists = response.body<List<PlaylistDto>>()
        assertEquals(0, playlists.size)
    }

    @Test
    fun testGetPlaylistsContainingTrack_OnlyOwnedPlaylists() = testApp(false) { client ->
        val (_, token1) = UserTestUtils.createUserWithSession("user1", "password")
        val (user2, token2) = UserTestUtils.createUserWithSession("user2", "password")

        val publicPlaylist = PlaylistTestUtils.createPlaylist("Public Playlist", true, user2.id.value)
        val privatePlaylist = PlaylistTestUtils.createPlaylist("Private Playlist", false, user2.id.value)
        val track = TrackTestUtils.createTrack("Test Track", "Test Album", "Test Artist")

        // Add track to both playlists
        client.post("/api/playlists/${publicPlaylist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token2)
        }
        client.post("/api/playlists/${privatePlaylist.id.value}/tracks") {
            parameter("trackId", track.id.value)
            bearerAuth(token2)
        }

        // user1 should not see any playlists (none are owned or collaborated on)
        val response = client.get("/api/playlists/containing/${track.id.value}") {
            bearerAuth(token1)
        }

        assertTrue(response.status.isSuccess())

        val playlists = response.body<List<PlaylistDto>>()
        assertEquals(0, playlists.size)
    }

    // endregion
}