package rest

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import utils.AlbumTestUtils
import utils.TrackTestUtils
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.albums.AlbumDataDto
import wtf.ndu.vibin.dto.albums.AlbumDto
import wtf.ndu.vibin.dto.albums.AlbumEditDto
import wtf.ndu.vibin.permissions.PermissionType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AlbumRestTest {

    // region Get

    @Test
    fun testGetAlbums() = testApp { client ->
        AlbumTestUtils.createAlbum("A Test Album 1", "Description 1", 2020)
        AlbumTestUtils.createAlbum("B Test Album 2", "Description 2", 2021)
        AlbumTestUtils.createAlbum("C Another Album", "Description 3", 2022)

        val response = client.get("/api/albums") {
            parameter("pageSize", 2)
        }

        assertTrue(response.status.isSuccess())

        val paginatedResponse = response.body<PaginatedDto<AlbumDto>>()

        assertEquals(3, paginatedResponse.total)
        assertEquals(2, paginatedResponse.items.size)

        val firstAlbum = paginatedResponse.items[0]
        assertEquals("A Test Album 1", firstAlbum.title)
        assertEquals("Description 1", firstAlbum.description)
        assertEquals(2020, firstAlbum.year)

        val secondAlbum = paginatedResponse.items[1]
        assertEquals("B Test Album 2", secondAlbum.title)
        assertEquals("Description 2", secondAlbum.description)
        assertEquals(2021, secondAlbum.year)
    }

    @Test
    fun testGetAlbums_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_ALBUMS to false
        )

        val response = client.get("/api/albums") {
            parameter("pageSize", 2)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetAlbums_Search() = testApp { client ->
        AlbumTestUtils.createAlbum("A test Album 1", "Description 1", 2020)
        AlbumTestUtils.createAlbum("Test Album 2", "Description 2", 2021)
        AlbumTestUtils.createAlbum("C Another Album", "Description 3", 2022)

        val response = client.get("/api/albums") {
            parameter("query", "Test")
        }

        assertTrue(response.status.isSuccess())

        val paginatedResponse = response.body<PaginatedDto<AlbumDto>>()

        assertEquals(2, paginatedResponse.total)
        assertEquals(2, paginatedResponse.items.size)

        val firstAlbum = paginatedResponse.items[0]
        assertEquals("A test Album 1", firstAlbum.title)
        assertEquals("Description 1", firstAlbum.description)
        assertEquals(2020, firstAlbum.year)

        val secondAlbum = paginatedResponse.items[1]
        assertEquals("Test Album 2", secondAlbum.title)
        assertEquals("Description 2", secondAlbum.description)
        assertEquals(2021, secondAlbum.year)
    }

    @Test
    fun testGetAlbumData() = testApp { client ->
        val album = AlbumTestUtils.createAlbum("test", "A description", 2020)

        TrackTestUtils.createTrack("Track 1", album = "test", artists = "Artist 1", trackNumber = 1, discNumber = 1)
        TrackTestUtils.createTrack("Track 2", album = "test", artists = "Artist 1, Artist 3", trackNumber = 2, discNumber = 1)
        TrackTestUtils.createTrack("Track 3", album = "test", artists = "Artist 2", trackNumber = 1, discNumber = 2)

        val response = client.get("/api/albums/${album.id.value}")

        assertTrue(response.status.isSuccess())

        val albumData = response.body<AlbumDataDto>()

        assertEquals("test", albumData.album.title)
        assertEquals("A description", albumData.album.description)
        assertEquals(2020, albumData.album.year)
        assertEquals(3, albumData.album.trackCount)

        assertEquals(3, albumData.album.artists.size)
        assertContains(albumData.album.artists.map { it.name }, "Artist 1")
        assertContains(albumData.album.artists.map { it.name }, "Artist 2")
        assertContains(albumData.album.artists.map { it.name }, "Artist 3")

        assertEquals(3, albumData.tracks.size)

        val track1 = albumData.tracks[0]
        assertEquals("Track 1", track1.title)
        assertEquals(1, track1.trackNumber)
        assertEquals(1, track1.discNumber)
        assertEquals(1, track1.artists.size)
        assertEquals("Artist 1", track1.artists[0].name)

        val track2 = albumData.tracks[1]
        assertEquals("Track 2", track2.title)
        assertEquals(2, track2.trackNumber)
        assertEquals(1, track2.discNumber)
        assertEquals(2, track2.artists.size)
        assertEquals(track2.artists[0].name, "Artist 1")
        assertEquals(track2.artists[1].name, "Artist 3")

        val track3 = albumData.tracks[2]
        assertEquals("Track 3", track3.title)
        assertEquals(1, track3.trackNumber)
        assertEquals(2, track3.discNumber)
        assertEquals(1, track3.artists.size)
        assertEquals("Artist 2", track3.artists[0].name)
    }

    @Test
    fun testGetAlbumData_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_ALBUMS to false
        )

        val album = AlbumTestUtils.createAlbum("test", "A description", 2020)

        val response = client.get("/api/albums/${album.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }
    // endregion

    // region Update

    @Test
    fun testUpdateAlbum() = testApp { client ->
        val album = AlbumTestUtils.createAlbum("Old Title", "Old Description", 2000)

        val updateData = AlbumEditDto(
            title = "New Title",
            description = "New Description",
            year = 2021,
            coverUrl = null
        )

        val response = client.put("/api/albums/${album.id.value}") {
            setBody(updateData)
        }

        assertTrue(response.status.isSuccess())

        val updatedAlbum = response.body<AlbumDto>()
        assertEquals("New Title", updatedAlbum.title)
        assertEquals("New Description", updatedAlbum.description)
        assertEquals(2021, updatedAlbum.year)
    }

    @Test
    fun testUpdateAlbum_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_ALBUMS to false
        )

        val album = AlbumTestUtils.createAlbum("Old Title", "Old Description", 2000)

        val updateData = AlbumEditDto(
            title = "New Title",
            description = "New Description",
            year = 2021,
            coverUrl = null
        )

        val response = client.put("/api/albums/${album.id.value}") {
            bearerAuth(token)
            setBody(updateData)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testUpdateAlbum_EmptyTitle() = testApp { client ->
        val album = AlbumTestUtils.createAlbum("Old Title", "Old Description", 2000)

        val updateData = AlbumEditDto(
            title = "",
            description = "New Description",
            year = 2021,
            coverUrl = null
        )

        val response = client.put("/api/albums/${album.id.value}") {
            setBody(updateData)
        }

        assertTrue(response.status.isSuccess())

        val updatedAlbum = response.body<AlbumDto>()
        assertEquals("Old Title", updatedAlbum.title) // Title should remain unchanged
        assertEquals("New Description", updatedAlbum.description)
        assertEquals(2021, updatedAlbum.year)
    }

    @Test
    fun testUpdateAlbum_NotFound() = testApp { client ->
        val updateData = AlbumEditDto(
            title = "New Title",
            description = "New Description",
            year = 2021,
            coverUrl = null
        )

        val response = client.put("/api/albums/9999") {
            setBody(updateData)
        }

        assertEquals(404, response.status.value)
    }
    // endregion

    // region Autocomplete

    @Test
    fun testAutocompleteAlbums() = testApp { client ->
        AlbumTestUtils.createAlbum("My test album", "Description 1", 2020)
        AlbumTestUtils.createAlbum("Another album", "Description 2", 2021)
        AlbumTestUtils.createAlbum("This is my other album", "Description 3", 2022)

        val response = client.get("/api/albums/autocomplete") {
            parameter("query", "my")
            parameter("limit", 5)
        }

        assertTrue(response.status.isSuccess())

        val suggestions = response.body<List<String>>()

        assertEquals(2, suggestions.size)

        assertEquals("My test album", suggestions[0])
        assertEquals("This is my other album", suggestions[1])
    }

    @Test
    fun testAutocompleteAlbums_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_ALBUMS to false
        )

        val response = client.get("/api/albums/autocomplete") {
            parameter("query", "my")
            parameter("limit", 5)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }
    // endregion
}