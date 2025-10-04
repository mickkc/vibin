package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import utils.ArtistTestUtils
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.dto.ArtistDto
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.artists.ArtistEditData
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.ArtistRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArtistRestTest {

    // region Get

    @Test
    fun testGetArtists() = testApp { client ->
        val artist1 = ArtistTestUtils.createArtist("B", "This should be second")
        val artist2 = ArtistTestUtils.createArtist("A", "This should be first")
        ArtistTestUtils.createArtist("C", "This should be third")

        val response = client.get("/api/artists") {
            parameter("pageSize", 2)
        }
        assertTrue(response.status.isSuccess())

        val data = response.body<PaginatedDto<ArtistDto>>()

        assertEquals(3, data.total)
        assertEquals(2, data.items.size)
        assertEquals(1, data.currentPage)
        assertEquals(2, data.pageSize)
        assertTrue(data.hasNext)
        assertFalse(data.hasPrevious)

        val artistTwo = data.items[0]
        assertEquals(artist2.name, artistTwo.name)
        assertEquals(artist2.description, artistTwo.description)

        val artistOne = data.items[1]
        assertEquals(artist1.name, artistOne.name)
        assertEquals(artist1.description, artistOne.description)
    }

    @Test
    fun testGetArtists_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms",
            "password",
            PermissionType.VIEW_ARTISTS to false
        )
        val response = client.get("/api/artists") {
            bearerAuth(token)
        }
        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetArtists_Search() = testApp { client ->
        ArtistTestUtils.createArtist("Match 1")
        ArtistTestUtils.createArtist("Should not appear")
        ArtistTestUtils.createArtist("this is match 2")

        val response = client.get("/api/artists") {
            parameter("query", "Match")
        }
        assertTrue(response.status.isSuccess())

        val data = response.body<PaginatedDto<ArtistDto>>()
        assertEquals(2, data.total)
        assertEquals(2, data.items.size)
        assertEquals("Match 1", data.items[0].name)
        assertEquals("this is match 2", data.items[1].name)
    }
    // endregion

    // region Create

    @Test
    fun testCreateArtist() = testApp { client ->
        val data = ArtistEditData(
            name = "New Artist",
            description = "An awesome new artist",
            imageUrl = null
        )

        val response = client.post("/api/artists") {
            setBody(data)
        }
        assertTrue(response.status.isSuccess())

        val createdArtist = response.body<ArtistDto>()
        assertEquals("New Artist", createdArtist.name)
        assertEquals("An awesome new artist", createdArtist.description)
    }

    @Test
    fun testCreateArtist_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms2", "password",
            PermissionType.MANAGE_ARTISTS to false
        )
        val data = ArtistEditData(
            name = "New Artist",
            description = "An awesome new artist",
            imageUrl = null
        )

        val response = client.post("/api/artists") {
            bearerAuth(token)
            setBody(data)
        }
        assertEquals(403, response.status.value)

        assertEquals(0, ArtistRepo.count())
    }

    // endregion

    // region Update

    @Test
    fun testUpdateArtist() = testApp { client ->
        val artist = ArtistTestUtils.createArtist("Old Name", "An old description")

        val data = ArtistEditData(
            name = "Updated Name",
            description = "An updated description",
            imageUrl = null
        )

        val response = client.put("/api/artists/${artist.id.value}") {
            setBody(data)
        }
        assertTrue(response.status.isSuccess())

        val updatedArtist = response.body<ArtistDto>()
        assertEquals(artist.id.value, updatedArtist.id)
        assertEquals("Updated Name", updatedArtist.name)
        assertEquals("An updated description", updatedArtist.description)
    }

    @Test
    fun testUpdateArtist_NotFound() = testApp { client ->
        val data = ArtistEditData(
            name = "Updated Name",
            description = "An updated description",
            imageUrl = null
        )

        val response = client.put("/api/artists/999999") {
            setBody(data)
        }
        assertEquals(404, response.status.value)
        assertEquals(0, ArtistRepo.count())
    }

    @Test
    fun testUpdateArtist_NoPermission() = testApp(false) { client ->
        val artist = ArtistTestUtils.createArtist("No Perms", "This should not change")
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms3", "password",
            PermissionType.MANAGE_ARTISTS to false
        )

        val data = ArtistEditData(
            name = "Updated Name",
            description = "An updated description",
            imageUrl = null
        )

        val response = client.put("/api/artists/${artist.id.value}") {
            bearerAuth(token)
            setBody(data)
        }
        assertEquals(403, response.status.value)

        val notUpdatedArtist = ArtistRepo.getById(artist.id.value)
        assertNotNull(notUpdatedArtist)
        assertEquals("No Perms", notUpdatedArtist.name)
        assertEquals("This should not change", notUpdatedArtist.description)
    }
    // endregion

    // region Delete

    @Test
    fun testDeleteArtist() = testApp { client ->
        val artist = ArtistTestUtils.createArtist("To Be Deleted")
        assertEquals(1, ArtistRepo.count())

        val response = client.delete("/api/artists/${artist.id.value}")
        assertTrue(response.status.isSuccess())

        val deletedArtist = ArtistRepo.getById(artist.id.value)
        assertNull(deletedArtist)
        assertEquals(0, ArtistRepo.count())
    }

    @Test
    fun testDeleteArtist_NotFound() = testApp { client ->
        val response = client.delete("/api/artists/999999")
        assertEquals(404, response.status.value)
    }

    @Test
    fun testDeleteArtist_NoPermission() = testApp(false) { client ->
        val artist = ArtistTestUtils.createArtist("Not Deleted")
        assertEquals(1, ArtistRepo.count())

        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms4", "password",
            PermissionType.MANAGE_ARTISTS to false
        )

        val response = client.delete("/api/artists/${artist.id.value}") {
            bearerAuth(token)
        }
        assertEquals(403, response.status.value)

        val notDeletedArtist = ArtistRepo.getById(artist.id.value)
        assertNotNull(notDeletedArtist)
        assertEquals(1, ArtistRepo.count())
    }

    // endregion

    // region Autocomplete

    @Test
    fun testAutocompleteArtists() = testApp { client ->
        ArtistTestUtils.createArtist("The Beatles")
        ArtistTestUtils.createArtist("The Rolling Stones")
        ArtistTestUtils.createArtist("Contains the Word")
        ArtistTestUtils.createArtist("No Match")

        val response = client.get("/api/artists/autocomplete") {
            parameter("query", "The")
            parameter("limit", 5)
        }
        assertTrue(response.status.isSuccess())

        val suggestions = response.body<List<String>>()
        assertEquals(3, suggestions.size)
        assertEquals("The Beatles", suggestions[0])
        assertEquals("The Rolling Stones", suggestions[1])
        assertEquals("Contains the Word", suggestions[2])
    }

    @Test
    fun testAutocompleteArtists_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            "noperms5", "password",
            PermissionType.VIEW_ARTISTS to false
        )
        ArtistTestUtils.createArtist("The Beatles")

        val response = client.get("/api/artists/autocomplete") {
            bearerAuth(token)
            parameter("query", "The")
            parameter("limit", 5)
        }
        assertEquals(403, response.status.value)
    }
    // endregion
}