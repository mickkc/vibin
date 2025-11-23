package rest

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import utils.AlbumTestUtils
import utils.ArtistTestUtils
import utils.TrackTestUtils
import utils.UserTestUtils
import utils.testApp
import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.dto.FavoriteDto
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.repos.FavoriteRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FavoriteRestTest {

    @Serializable
    data class FavoriteCheckResponse(
        val isFavorite: Boolean,
        val place: Int?
    )

    // region Get

    @Test
    fun testGetFavorites() = testApp { client ->

        val track1 = TrackTestUtils.createTrack("Track 1", "Album 1", "Artist 1")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album 2", "Artist 2")
        val album1 = AlbumTestUtils.createAlbum("Favorite Album 1")
        val album2 = AlbumTestUtils.createAlbum("Favorite Album 2")
        val artist1 = ArtistTestUtils.createArtist("Favorite Artist 1")
        val artist2 = ArtistTestUtils.createArtist("Favorite Artist 2")

        FavoriteRepo.addFavorite(1, FavoriteType.TRACK, track1.id.value, 1)
        FavoriteRepo.addFavorite(1, FavoriteType.TRACK, track2.id.value, 2)

        FavoriteRepo.addFavorite(1, FavoriteType.ALBUM, album1.id.value, 1)
        FavoriteRepo.addFavorite(1, FavoriteType.ALBUM, album2.id.value, 3)

        FavoriteRepo.addFavorite(1, FavoriteType.ARTIST, artist1.id.value, 2)
        FavoriteRepo.addFavorite(1, FavoriteType.ARTIST, artist2.id.value, 3)

        // Get favorites
        val response = client.get("/api/favorites/1")

        assertTrue(response.status.isSuccess())

        val favorites = response.body<FavoriteDto>()

        // Check tracks
        assertEquals(3, favorites.tracks.size)
        assertNotNull(favorites.tracks[0])
        assertEquals("Track 1", favorites.tracks[0]?.title)
        assertNotNull(favorites.tracks[1])
        assertEquals("Track 2", favorites.tracks[1]?.title)
        assertNull(favorites.tracks[2])

        // Check albums
        assertEquals(3, favorites.albums.size)
        assertNotNull(favorites.albums[0])
        assertEquals("Favorite Album 1", favorites.albums[0]?.title)
        assertNull(favorites.albums[1])
        assertNotNull(favorites.albums[2])
        assertEquals("Favorite Album 2", favorites.albums[2]?.title)

        // Check artists
        assertEquals(3, favorites.artists.size)
        assertNull(favorites.artists[0])
        assertNotNull(favorites.artists[1])
        assertEquals("Favorite Artist 1", favorites.artists[1]?.name)
        assertNotNull(favorites.artists[2])
        assertEquals("Favorite Artist 2", favorites.artists[2]?.name)
    }

    @Test
    fun testGetFavorites_EmptyFavorites() = testApp { client ->

        val response = client.get("/api/favorites/1")

        assertTrue(response.status.isSuccess())

        val favorites = response.body<FavoriteDto>()

        assertEquals(3, favorites.tracks.size)
        assertTrue(favorites.tracks.all { it == null })
        assertEquals(3, favorites.albums.size)
        assertTrue(favorites.albums.all { it == null })
        assertEquals(3, favorites.artists.size)
        assertTrue(favorites.artists.all { it == null })
    }

    @Test
    fun testGetFavorites_NoPermission() = testApp(false) { client ->
        val (user, _) = UserTestUtils.createUserWithSession("targetuser", "password")
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_USERS to false
        )

        val response = client.get("/api/favorites/${user.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }
    // endregion

    // region Check

    @Test
    fun testCheckFavorite() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        // Initially not a favorite
        val initialCheckResponse = client.get("/api/favorites/track/check/${track.id.value}")
        val initialCheck = initialCheckResponse.body<FavoriteCheckResponse>()

        assertFalse(initialCheck.isFavorite)
        assertNull(initialCheck.place)

        // Add as favorite
        client.put("/api/favorites/track/1") {
            parameter("entityId", track.id.value)
        }

        // Check again
        val checkResponse = client.get("/api/favorites/track/check/${track.id.value}")
        val check = checkResponse.body<FavoriteCheckResponse>()

        assertTrue(check.isFavorite)
        assertEquals(1, check.place)
    }

    @Test
    fun testCheckFavorite_NoPermission() = testApp(false) { client ->
        val (user, _) = UserTestUtils.createUserAndSessionWithPermissions(
            "targetuser", "password",
            PermissionType.MANAGE_OWN_USER to false
        )

        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")
        FavoriteRepo.addFavorite(user.id.value, FavoriteType.TRACK, track.id.value, 1)

        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_USERS to false
        )

        val response = client.get("/api/favorites/track/check/${track.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // region Add

    @Test
    fun testAddFavoriteTrack() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.put("/api/favorites/track/1") {
            parameter("entityId", track.id.value)
        }

        assertTrue(response.status.isSuccess())

        // Verify it was added
        val getFavoritesResponse = client.get("/api/favorites/1")
        val favorites = getFavoritesResponse.body<FavoriteDto>()

        assertNotNull(favorites.tracks[0])
        assertEquals("Test Track", favorites.tracks[0]?.title)

        val isFavoriteCheckResponse = client.get("/api/favorites/track/check/${track.id.value}")
        val checkResult = isFavoriteCheckResponse.body<FavoriteCheckResponse>()

        assertTrue(checkResult.isFavorite)
        assertEquals(1, checkResult.place)
    }

    @Test
    fun testAddFavoriteAlbum() = testApp { client ->
        val album = AlbumTestUtils.createAlbum("Test Album")

        val response = client.put("/api/favorites/album/2") {
            parameter("entityId", album.id.value)
        }

        assertTrue(response.status.isSuccess())

        // Verify it was added
        val getFavoritesResponse = client.get("/api/favorites/1")
        val favorites = getFavoritesResponse.body<FavoriteDto>()

        assertNotNull(favorites.albums[1])
        assertEquals("Test Album", favorites.albums[1]?.title)

        val isFavoriteCheckResponse = client.get("/api/favorites/album/check/${album.id.value}")
        val isFavorite = isFavoriteCheckResponse.body<FavoriteCheckResponse>()

        assertTrue(isFavorite.isFavorite)
        assertEquals(2, isFavorite.place)
    }

    @Test
    fun testAddFavoriteArtist() = testApp { client ->
        val artist = ArtistTestUtils.createArtist("Test Artist")

        val response = client.put("/api/favorites/artist/3") {
            parameter("entityId", artist.id.value)
        }

        assertTrue(response.status.isSuccess())

        // Verify it was added
        val getFavoritesResponse = client.get("/api/favorites/1")
        val favorites = getFavoritesResponse.body<FavoriteDto>()

        assertNotNull(favorites.artists[2])
        assertEquals("Test Artist", favorites.artists[2]?.name)

        val isFavoriteCheckResponse = client.get("/api/favorites/artist/check/${artist.id.value}")
        val isFavorite = isFavoriteCheckResponse.body<FavoriteCheckResponse>()

        assertTrue(isFavorite.isFavorite)
        assertEquals(3, isFavorite.place)
    }

    @Test
    fun testAddFavorite_ReplaceAtSamePlace() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        // Add first track at place 1
        client.put("/api/favorites/track/1") {
            parameter("entityId", track1.id.value)
        }

        val getFavoritesResponseBefore = client.get("/api/favorites/1")
        val favoritesBefore = getFavoritesResponseBefore.body<FavoriteDto>()

        assertNotNull(favoritesBefore.tracks[0])
        assertEquals("Track 1", favoritesBefore.tracks[0]?.title)

        // Add second track at place 1 (should replace)
        val response = client.put("/api/favorites/track/1") {
            parameter("entityId", track2.id.value)
        }

        assertTrue(response.status.isSuccess())

        // Verify only the second track is at place 1
        val getFavoritesResponse = client.get("/api/favorites/1")
        val favorites = getFavoritesResponse.body<FavoriteDto>()

        assertNotNull(favorites.tracks[0])
        assertEquals("Track 2", favorites.tracks[0]?.title)
        assertNull(favorites.tracks[1])
        assertNull(favorites.tracks[2])
    }

    @Test
    fun testAddFavorite_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_OWN_USER to false
        )

        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.put("/api/favorites/track/1") {
            bearerAuth(token)
            parameter("entityId", track.id.value)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testAddFavorite_InvalidPlace() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        // Test place 0
        val response1 = client.put("/api/favorites/track/0") {
            parameter("entityId", track.id.value)
        }
        assertEquals(400, response1.status.value)

        // Test place 4
        val response2 = client.put("/api/favorites/track/4") {
            parameter("entityId", track.id.value)
        }
        assertEquals(400, response2.status.value)
    }

    @Test
    fun testAddFavorite_InvalidEntityType() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        val response = client.put("/api/favorites/invalid/1") {
            parameter("entityId", track.id.value)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testAddFavorite_MissingEntityId() = testApp { client ->
        val response = client.put("/api/favorites/track/1")

        assertEquals(400, response.status.value)
    }
    // endregion

    // region Delete

    @Test
    fun testDeleteFavorite() = testApp { client ->
        val track = TrackTestUtils.createTrack("Test Track", "Album", "Artist")

        // Add favorite
        client.put("/api/favorites/track/1") {
            parameter("entityId", track.id.value)
        }

        // Delete favorite
        val response = client.delete("/api/favorites/track/1")

        assertTrue(response.status.isSuccess())

        // Verify it was deleted
        val getFavoritesResponse = client.get("/api/favorites/1")
        val favorites = getFavoritesResponse.body<FavoriteDto>()

        assertNull(favorites.tracks[0])
    }

    @Test
    fun testDeleteFavorite_NonExistent() = testApp { client ->
        // Try to delete a favorite that doesn't exist (should still succeed)
        val response = client.delete("/api/favorites/track/1")

        assertTrue(response.status.isSuccess())
    }

    @Test
    fun testDeleteFavorite_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_OWN_USER to false
        )

        val response = client.delete("/api/favorites/track/1") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testDeleteFavorite_InvalidPlace() = testApp { client ->
        // Test place 0
        val response1 = client.delete("/api/favorites/track/0")
        assertEquals(400, response1.status.value)

        // Test place 4
        val response2 = client.delete("/api/favorites/track/4")
        assertEquals(400, response2.status.value)
    }

    @Test
    fun testDeleteFavorite_InvalidEntityType() = testApp { client ->
        val response = client.delete("/api/favorites/invalid/1")

        assertEquals(400, response.status.value)
    }
    // endregion
}