package rest

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import utils.AlbumTestUtils
import utils.ArtistTestUtils
import utils.TagTestUtils
import utils.UserTestUtils
import utils.testApp
import wtf.ndu.vibin.dto.ArtistDto
import wtf.ndu.vibin.dto.CreateMetadataDto
import wtf.ndu.vibin.dto.albums.AlbumDto
import wtf.ndu.vibin.dto.tags.TagDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.AlbumRepo
import wtf.ndu.vibin.repos.ArtistRepo
import wtf.ndu.vibin.repos.TagRepo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetadataRestTest {

    @Serializable
    private data class CreateMetadataResponse(
        val artists: List<ArtistDto>,
        val tags: List<TagDto>,
        val album: AlbumDto?
    )

    // region Create Metadata

    @Test
    fun testCreateMetadata_NewEntities() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = listOf("New Artist 1", "New Artist 2"),
            tagNames = listOf("New Tag 1", "New Tag 2"),
            albumName = "New Album"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        // Verify artists were created
        assertEquals(2, result.artists.size)
        assertEquals("New Artist 1", result.artists[0].name)
        assertEquals("New Artist 2", result.artists[1].name)

        // Verify tags were created
        assertEquals(2, result.tags.size)
        assertEquals("New Tag 1", result.tags[0].name)
        assertEquals("New Tag 2", result.tags[1].name)

        // Verify album was created
        assertNotNull(result.album)
        assertEquals("New Album", result.album.title)
    }

    @Test
    fun testCreateMetadata_ExistingEntities() = testApp { client ->
        // Pre-create entities
        val existingArtist = ArtistTestUtils.createArtist("Existing Artist", "An artist description")
        val existingTag = TagTestUtils.createTag("Existing Tag", "A tag description")
        val existingAlbum = AlbumTestUtils.createAlbum("Existing Album", "An album description", 2020)

        val request = CreateMetadataDto(
            artistNames = listOf("Existing Artist"),
            tagNames = listOf("Existing Tag"),
            albumName = "Existing Album"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        // Verify existing artists were returned (not duplicated)
        assertEquals(1, result.artists.size)
        assertEquals(existingArtist.id.value, result.artists[0].id)
        assertEquals("Existing Artist", result.artists[0].name)

        // Verify existing tags were returned (not duplicated)
        assertEquals(1, result.tags.size)
        assertEquals(existingTag.id.value, result.tags[0].id)
        assertEquals("Existing Tag", result.tags[0].name)

        // Verify existing album was returned (not duplicated)
        assertNotNull(result.album)
        assertEquals(existingAlbum.id.value, result.album.id)
        assertEquals("Existing Album", result.album.title)

        // Verify no new entities were created in the database
        assertEquals(1, ArtistRepo.count())
        assertEquals(1, TagRepo.count())
        assertEquals(1, AlbumRepo.count())
    }

    @Test
    fun testCreateMetadata_MixedNewAndExisting() = testApp { client ->
        // Pre-create some entities
        val existingArtist = ArtistTestUtils.createArtist("Existing Artist", "An artist description")
        val existingTag = TagTestUtils.createTag("Existing Tag", "A tag description")

        val request = CreateMetadataDto(
            artistNames = listOf("Existing Artist", "New Artist"),
            tagNames = listOf("Existing Tag", "New Tag"),
            albumName = "New Album"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        // Verify both existing and new artists
        assertEquals(2, result.artists.size)
        assertEquals(existingArtist.id.value, result.artists[0].id)
        assertEquals("Existing Artist", result.artists[0].name)
        assertEquals("New Artist", result.artists[1].name)

        // Verify both existing and new tags
        assertEquals(2, result.tags.size)
        assertEquals(existingTag.id.value, result.tags[0].id)
        assertEquals("Existing Tag", result.tags[0].name)
        assertEquals("New Tag", result.tags[1].name)

        // Verify new album was created
        assertNotNull(result.album)
        assertEquals("New Album", result.album.title)

        // Verify correct counts in database
        assertEquals(2, ArtistRepo.count())
        assertEquals(2, TagRepo.count())
        assertEquals(1, AlbumRepo.count())
    }

    @Test
    fun testCreateMetadata_EmptyLists() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = emptyList(),
            tagNames = emptyList(),
            albumName = null
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        assertEquals(0, result.artists.size)
        assertEquals(0, result.tags.size)
        assertNull(result.album)
    }

    @Test
    fun testCreateMetadata_OnlyArtists() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = listOf("Artist 1", "Artist 2", "Artist 3"),
            tagNames = emptyList(),
            albumName = null
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        assertEquals(3, result.artists.size)
        assertEquals("Artist 1", result.artists[0].name)
        assertEquals("Artist 2", result.artists[1].name)
        assertEquals("Artist 3", result.artists[2].name)
        assertEquals(0, result.tags.size)
        assertNull(result.album)
    }

    @Test
    fun testCreateMetadata_OnlyTags() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = emptyList(),
            tagNames = listOf("Tag 1", "Tag 2", "Tag 3"),
            albumName = null
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        assertEquals(0, result.artists.size)
        assertEquals(3, result.tags.size)
        assertEquals("Tag 1", result.tags[0].name)
        assertEquals("Tag 2", result.tags[1].name)
        assertEquals("Tag 3", result.tags[2].name)
        assertNull(result.album)
    }

    @Test
    fun testCreateMetadata_OnlyAlbum() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = emptyList(),
            tagNames = emptyList(),
            albumName = "Solo Album"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        assertEquals(0, result.artists.size)
        assertEquals(0, result.tags.size)
        assertNotNull(result.album)
        assertEquals("Solo Album", result.album.title)
    }

    @Test
    fun testCreateMetadata_CaseInsensitiveMatching() = testApp { client ->
        // Create entities with specific casing
        ArtistTestUtils.createArtist("Rock Artist", "A rock artist")
        TagTestUtils.createTag("Rock Genre", "Rock music genre")
        AlbumTestUtils.createAlbum("Best Of Rock", "A compilation", 2020)

        // Request with different casing
        val request = CreateMetadataDto(
            artistNames = listOf("rock artist", "ROCK ARTIST"),
            tagNames = listOf("ROCK GENRE", "rock genre"),
            albumName = "BEST OF ROCK"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        // Should return the same entity multiple times (not create duplicates)
        assertEquals(2, result.artists.size)
        assertEquals(result.artists[0].id, result.artists[1].id)
        assertEquals("Rock Artist", result.artists[0].name)

        assertEquals(2, result.tags.size)
        assertEquals(result.tags[0].id, result.tags[1].id)
        assertEquals("Rock Genre", result.tags[0].name)

        assertNotNull(result.album)
        assertEquals("Best Of Rock", result.album.title)

        // Verify no duplicates were created
        assertEquals(1, ArtistRepo.count())
        assertEquals(1, TagRepo.count())
        assertEquals(1, AlbumRepo.count())
    }

    @Test
    fun testCreateMetadata_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TRACKS to false
        )

        val request = CreateMetadataDto(
            artistNames = listOf("Artist"),
            tagNames = listOf("Tag"),
            albumName = "Album"
        )

        val response = client.post("/api/metadata/create") {
            bearerAuth(token)
            setBody(request)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testCreateMetadata_DuplicateNamesInRequest() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = listOf("Same Artist", "Same Artist", "Same Artist"),
            tagNames = listOf("Same Tag", "Same Tag"),
            albumName = "Album"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        // Should return the same entity multiple times
        assertEquals(3, result.artists.size)
        assertEquals(result.artists[0].id, result.artists[1].id)
        assertEquals(result.artists[0].id, result.artists[2].id)
        assertEquals("Same Artist", result.artists[0].name)

        assertEquals(2, result.tags.size)
        assertEquals(result.tags[0].id, result.tags[1].id)
        assertEquals("Same Tag", result.tags[0].name)

        // But only one entity should be created in the database
        assertEquals(1, ArtistRepo.count())
        assertEquals(1, TagRepo.count())
        assertEquals(1, AlbumRepo.count())
    }

    @Test
    fun testCreateMetadata_LargeNumberOfEntities() = testApp { client ->
        val artistNames = (1..50).map { "Artist $it" }
        val tagNames = (1..50).map { "Tag $it" }

        val request = CreateMetadataDto(
            artistNames = artistNames,
            tagNames = tagNames,
            albumName = "Compilation Album"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        assertEquals(50, result.artists.size)
        assertEquals(50, result.tags.size)
        assertNotNull(result.album)

        // Verify all were created
        assertEquals(50, ArtistRepo.count())
        assertEquals(50, TagRepo.count())
        assertEquals(1, AlbumRepo.count())
    }

    @Test
    fun testCreateMetadata_SpecialCharactersInNames() = testApp { client ->
        val request = CreateMetadataDto(
            artistNames = listOf("AC/DC", "Guns N' Roses", "Panic! at the Disco"),
            tagNames = listOf("80's Rock", "Pop-Punk", "Alt.Rock"),
            albumName = "Greatest Hits: 1980-2020"
        )

        val response = client.post("/api/metadata/create") {
            setBody(request)
        }

        assertTrue(response.status.isSuccess())

        val result = response.body<CreateMetadataResponse>()

        assertEquals(3, result.artists.size)
        assertEquals("AC/DC", result.artists[0].name)
        assertEquals("Guns N' Roses", result.artists[1].name)
        assertEquals("Panic! at the Disco", result.artists[2].name)

        assertEquals(3, result.tags.size)
        assertEquals("80's Rock", result.tags[0].name)
        assertEquals("Pop-Punk", result.tags[1].name)
        assertEquals("Alt.Rock", result.tags[2].name)

        assertNotNull(result.album)
        assertEquals("Greatest Hits: 1980-2020", result.album.title)
    }

    // endregion
}
