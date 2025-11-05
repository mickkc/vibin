package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.transactions.transaction
import utils.*
import wtf.ndu.vibin.dto.PaginatedDto
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto
import wtf.ndu.vibin.dto.tracks.TrackDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.repos.LyricsRepo
import wtf.ndu.vibin.repos.TrackRepo
import kotlin.test.*

class TrackRestTest {

    @Serializable
    private data class LyricsResponse(
        val lyrics: String?,
        val colorScheme: Map<String, String>?
    )

    @Serializable
    private data class LyricsCheckResponse(
        val success: Boolean
    )

    // region Get All Tracks

    @Test
    fun testGetTracks() = testApp { client ->
        TrackTestUtils.createTrack("Track 1", "Album 1", "Artist 1", trackNumber = 1)
        TrackTestUtils.createTrack("Track 2", "Album 2", "Artist 2", trackNumber = 2)
        TrackTestUtils.createTrack("Track 3", "Album 3", "Artist 3", trackNumber = 3)

        val response = client.get("/api/tracks") {
            parameter("pageSize", 2)
        }

        assertTrue(response.status.isSuccess())

        val paginatedResponse = response.body<PaginatedDto<MinimalTrackDto>>()

        assertEquals(3, paginatedResponse.total)
        assertEquals(2, paginatedResponse.items.size)
        assertEquals(1, paginatedResponse.currentPage)
        assertEquals(2, paginatedResponse.pageSize)

        val firstTrack = paginatedResponse.items[0]
        assertNotNull(firstTrack.id)
        assertEquals("Track 1", firstTrack.title)
        assertEquals("Album 1", firstTrack.album.name)
        assertEquals(1, firstTrack.artists.size)
        assertEquals("Artist 1", firstTrack.artists[0].name)

        val secondTrack = paginatedResponse.items[1]
        assertNotNull(secondTrack.id)
        assertEquals("Track 2", secondTrack.title)
        assertEquals("Album 2", secondTrack.album.name)
        assertEquals(1, secondTrack.artists.size)
        assertEquals("Artist 2", secondTrack.artists[0].name)
    }

    @Test
    fun testGetTracks_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val response = client.get("/api/tracks") {
            parameter("pageSize", 2)
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetTracks_Pagination() = testApp { client ->
        repeat(10) { i ->
            TrackTestUtils.createTrack("Track $i", "Album", "Artist")
        }

        val page1Response = client.get("/api/tracks") {
            parameter("page", 1)
            parameter("pageSize", 5)
        }

        assertTrue(page1Response.status.isSuccess())
        val page1 = page1Response.body<PaginatedDto<MinimalTrackDto>>()
        assertEquals(10, page1.total)
        assertEquals(5, page1.items.size)
        assertEquals(1, page1.currentPage)

        val page2Response = client.get("/api/tracks") {
            parameter("page", 2)
            parameter("pageSize", 5)
        }

        assertTrue(page2Response.status.isSuccess())
        val page2 = page2Response.body<PaginatedDto<MinimalTrackDto>>()
        assertEquals(10, page2.total)
        assertEquals(5, page2.items.size)
        assertEquals(2, page2.currentPage)
    }

    // endregion

    // region Get Track by ID

    @Test
    fun testGetTrackById() = testApp { client ->
        val track = TrackTestUtils.createTrack(
            title = "Test Track",
            album = "Test Album",
            artists = "Test Artist",
            trackNumber = 1,
            discNumber = 1,
            year = 2020,
            duration = 180000,
            bitrate = 320,
            channels = 2,
            sampleRate = 44100,
            comment = "A test comment"
        )

        val response = client.get("/api/tracks/${track.id.value}")

        assertTrue(response.status.isSuccess())

        val trackDto = response.body<TrackDto>()

        assertEquals(track.id.value, trackDto.id)
        assertEquals("Test Track", trackDto.title)
        assertEquals("Test Album", trackDto.album.title)
        assertEquals(1, trackDto.artists.size)
        assertEquals("Test Artist", trackDto.artists[0].name)
        assertEquals(1, trackDto.trackNumber)
        assertEquals(1, trackDto.discNumber)
        assertEquals(2020, trackDto.year)
        assertEquals(180000, trackDto.duration)
        assertEquals(320, trackDto.bitrate)
        assertEquals(2, trackDto.channels)
        assertEquals(44100, trackDto.sampleRate)
        assertEquals("A test comment", trackDto.comment)
    }

    @Test
    fun testGetTrackById_NotFound() = testApp { client ->
        val response = client.get("/api/tracks/9999")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testGetTrackById_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Get Tracks by Artist

    @Test
    fun testGetTracksByArtist() = testApp { client ->
        val artist = ArtistTestUtils.createArtist("Test Artist", "An artist")

        TrackTestUtils.createTrack("Track 1", "Album 1", "Test Artist")
        TrackTestUtils.createTrack("Track 2", "Album 2", "Test Artist")
        TrackTestUtils.createTrack("Track 3", "Album 3", "Other Artist")

        val response = client.get("/api/tracks/artists/${artist.id.value}")

        assertTrue(response.status.isSuccess())

        val tracks = response.body<List<TrackDto>>()

        assertEquals(2, tracks.size)
        assertEquals("Track 1", tracks[0].title)
        assertEquals("Track 2", tracks[1].title)
    }

    @Test
    fun testGetTracksByArtist_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val artist = ArtistTestUtils.createArtist("Test Artist", "An artist")

        val response = client.get("/api/tracks/artists/${artist.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Update Track

    @Test
    fun testUpdateTrack() = testApp { client ->
        val track = TrackTestUtils.createTrack(
            title = "Old Title",
            album = "Old Album",
            artists = "Old Artist",
            trackNumber = 1,
            discNumber = 1
        )

        val newArtist = ArtistTestUtils.createArtist("New Artist", "A new artist")
        val newAlbum = AlbumTestUtils.createAlbum("New Album", "A new album", 2021)
        val tag = TagTestUtils.createTag("Rock", "Rock genre")

        val editDto = TrackEditDto(
            title = "New Title",
            explicit = true,
            trackNumber = 2,
            trackCount = 10,
            discNumber = 2,
            discCount = 2,
            year = 2022,
            comment = "Updated comment",
            imageUrl = null,
            album = newAlbum.id.value,
            artists = listOf(newArtist.id.value),
            tags = listOf(tag.id.value),
            lyrics = "New lyrics"
        )

        val response = client.put("/api/tracks/${track.id.value}") {
            setBody(editDto)
        }

        assertTrue(response.status.isSuccess())

        val updatedTrack = response.body<TrackDto>()

        assertEquals("New Title", updatedTrack.title)
        assertEquals(true, updatedTrack.explicit)
        assertEquals(2, updatedTrack.trackNumber)
        assertEquals(10, updatedTrack.trackCount)
        assertEquals(2, updatedTrack.discNumber)
        assertEquals(2, updatedTrack.discCount)
        assertEquals(2022, updatedTrack.year)
        assertEquals("Updated comment", updatedTrack.comment)
        assertEquals("New Album", updatedTrack.album.title)
        assertEquals(1, updatedTrack.artists.size)
        assertEquals("New Artist", updatedTrack.artists[0].name)
        assertEquals(1, updatedTrack.tags.size)
        assertEquals("Rock", updatedTrack.tags[0].name)

        val updatedDbTrack = TrackRepo.getById(track.id.value)
        assertNotNull(updatedDbTrack)

        val lyrics = LyricsRepo.getLyrics(track)
        assertNotNull(lyrics)
        assertEquals("New lyrics", lyrics.content)

        transaction {
            assertEquals("New Title", updatedDbTrack.title)
            assertEquals(true, updatedDbTrack.explicit)
            assertEquals(2, updatedDbTrack.trackNumber)
            assertEquals(10, updatedDbTrack.trackCount)
            assertEquals(2, updatedDbTrack.discNumber)
            assertEquals(2, updatedDbTrack.discCount)
            assertEquals(2022, updatedDbTrack.year)
            assertEquals("Updated comment", updatedDbTrack.comment)
            assertEquals("New Album", updatedDbTrack.album.title)
            assertEquals(1, updatedDbTrack.artists.count())
            assertEquals("New Artist", updatedDbTrack.artists.first().name)
            assertEquals(1, updatedDbTrack.tags.count())
            assertEquals("Rock", updatedDbTrack.tags.first().name)
        }

    }

    @Test
    fun testUpdateTrack_Nulls() = testApp { client ->
        val track = TrackTestUtils.createTrack(
            title = "Original Title",
            album = "Original Album",
            artists = "Original Artist",
            trackNumber = 1,
            trackCount = 5,
            discNumber = 1,
            discCount = 1,
            year = 2020,
            comment = "Original comment",
        )

        val editDto = TrackEditDto(
            title = null,
            explicit = null,
            trackNumber = null,
            trackCount = null,
            discNumber = null,
            discCount = null,
            year = null,
            comment = null,
            imageUrl = null,
            album = null,
            artists = null,
            tags = null,
            lyrics = null
        )

        val response = client.put("/api/tracks/${track.id.value}") {
            setBody(editDto)
        }

        assertTrue(response.status.isSuccess())

        val updatedTrack = response.body<TrackDto>()

        assertEquals("Original Title", updatedTrack.title)
        assertEquals("Original Album", updatedTrack.album.title)
        assertEquals("Original Artist", updatedTrack.artists[0].name)
        assertNull(updatedTrack.trackNumber)
        assertNull(updatedTrack.trackCount)
        assertNull(updatedTrack.discNumber)
        assertNull(updatedTrack.discCount)
        assertNull(updatedTrack.year)
        assertEquals("Original comment", updatedTrack.comment)
    }

    @Test
    fun testUpdateTrack_NotFound() = testApp { client ->
        val editDto = TrackEditDto(
            title = "New Title",
            explicit = null,
            trackNumber = null,
            trackCount = null,
            discNumber = null,
            discCount = null,
            year = null,
            comment = null,
            imageUrl = null,
            album = null,
            artists = null,
            tags = null,
            lyrics = null
        )

        val response = client.put("/api/tracks/9999") {
            setBody(editDto)
        }

        assertEquals(404, response.status.value)
    }

    @Test
    fun testUpdateTrack_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.MANAGE_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val editDto = TrackEditDto(
            title = "New Title",
            explicit = null,
            trackNumber = null,
            trackCount = null,
            discNumber = null,
            discCount = null,
            year = null,
            comment = null,
            imageUrl = null,
            album = null,
            artists = null,
            tags = null,
            lyrics = null
        )

        val response = client.put("/api/tracks/${track.id.value}") {
            bearerAuth(token)
            setBody(editDto)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Delete Track

    @Test
    fun testDeleteTrack() = testApp { client ->
        val track = TrackTestUtils.createTrack("Track to Delete", "Album", "Artist")

        val response = client.delete("/api/tracks/${track.id.value}")

        assertTrue(response.status.isSuccess())

        val getResponse = client.get("/api/tracks/${track.id.value}")
        assertEquals(404, getResponse.status.value)

        assertEquals(0, TrackRepo.count())
    }

    @Test
    fun testDeleteTrack_NotFound() = testApp { client ->
        val response = client.delete("/api/tracks/9999")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testDeleteTrack_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.DELETE_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.delete("/api/tracks/${track.id.value}") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Search Tracks

    @Test
    fun testSearchTracks() = testApp { client ->
        TrackTestUtils.createTrack("Rock Song", "Album", "Artist")
        TrackTestUtils.createTrack("Jazz Song", "Album", "Artist")
        TrackTestUtils.createTrack("Rock Ballad", "Album", "Artist")

        val response = client.get("/api/tracks/search") {
            parameter("query", "Rock")
            parameter("pageSize", 10)
        }

        assertTrue(response.status.isSuccess())

        val searchResults = response.body<PaginatedDto<MinimalTrackDto>>()

        assertEquals(2, searchResults.total)
        assertEquals(2, searchResults.items.size)
    }

    @Test
    fun testSearchTracks_NoResults() = testApp { client ->
        TrackTestUtils.createTrack("Track 1", "Album", "Artist")

        val response = client.get("/api/tracks/search") {
            parameter("query", "NonExistent")
            parameter("pageSize", 10)
        }

        assertTrue(response.status.isSuccess())

        val searchResults = response.body<PaginatedDto<MinimalTrackDto>>()

        assertEquals(0, searchResults.total)
        assertEquals(0, searchResults.items.size)
    }

    @Test
    fun testSearchTracks_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val response = client.get("/api/tracks/search") {
            parameter("query", "query")
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Random Tracks

    @Test
    fun testGetRandomTracks() = testApp { client ->
        repeat(10) { i ->
            TrackTestUtils.createTrack("Track $i", "Album", "Artist")
        }

        val response = client.get("/api/tracks/random") {
            parameter("limit", 5)
        }

        assertTrue(response.status.isSuccess())

        val randomTracks = response.body<List<MinimalTrackDto>>()

        assertTrue(randomTracks.size <= 5)
    }

    @Test
    fun testGetRandomTracks_LimitExceedsTotal() = testApp { client ->
        repeat(3) { i ->
            TrackTestUtils.createTrack("Track $i", "Album", "Artist")
        }

        val response = client.get("/api/tracks/random") {
            parameter("limit", 10)
        }

        assertTrue(response.status.isSuccess())

        val randomTracks = response.body<List<MinimalTrackDto>>()

        assertEquals(3, randomTracks.size)
    }

    @Test
    fun testGetRandomTracks_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val response = client.get("/api/tracks/random") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Newest Tracks

    @Test
    fun testGetNewestTracks() = testApp { client ->
        repeat(10) { i ->
            TrackTestUtils.createTrack("Track $i", "Album", "Artist")
        }

        val response = client.get("/api/tracks/newest") {
            parameter("limit", 5)
        }

        assertTrue(response.status.isSuccess())

        val newestTracks = response.body<List<MinimalTrackDto>>()

        assertTrue(newestTracks.size <= 5)
    }

    @Test
    fun testGetNewestTracks_LimitExceedsTotal() = testApp { client ->
        repeat(3) { i ->
            TrackTestUtils.createTrack("Track $i", "Album", "Artist")
        }

        val response = client.get("/api/tracks/newest") {
            parameter("limit", 10)
        }

        assertTrue(response.status.isSuccess())

        val newestTracks = response.body<List<MinimalTrackDto>>()

        assertEquals(3, newestTracks.size)
    }

    @Test
    fun testGetNewestTracks_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val response = client.get("/api/tracks/newest") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Lyrics

    @Test
    fun testGetTrackLyrics() = testApp { client ->
        val track = TrackTestUtils.createTrack("Track with Lyrics", "Album", "Artist")
        LyricsRepo.setLyrics(track, "These are the lyrics\nLine 2\nLine 3")

        val response = client.get("/api/tracks/${track.id.value}/lyrics")

        assertTrue(response.status.isSuccess())

        val lyricsResponse = response.body<LyricsResponse>()

        assertNotNull(lyricsResponse.lyrics)
        assertEquals("These are the lyrics\nLine 2\nLine 3", lyricsResponse.lyrics)
    }

    @Test
    fun testGetTrackLyrics_NoLyrics() = testApp { client ->
        val track = TrackTestUtils.createTrack("Track without Lyrics", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}/lyrics")

        assertTrue(response.status.isSuccess())

        val lyricsResponse = response.body<LyricsResponse>()

        assertNull(lyricsResponse.lyrics)
    }

    @Test
    fun testGetTrackLyrics_NotFound() = testApp { client ->
        val response = client.get("/api/tracks/9999/lyrics")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testGetTrackLyrics_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}/lyrics") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testCheckTrackHasLyrics() = testApp { client ->
        val trackWithLyrics = TrackTestUtils.createTrack("Track with Lyrics", "Album", "Artist")
        LyricsRepo.setLyrics(trackWithLyrics, "Lyrics content")

        val trackWithoutLyrics = TrackTestUtils.createTrack("Track without Lyrics", "Album", "Artist")

        val responseWithLyrics = client.get("/api/tracks/${trackWithLyrics.id.value}/lyrics/check")
        assertTrue(responseWithLyrics.status.isSuccess())

        val lyricsCheckWithLyrics = responseWithLyrics.body<LyricsCheckResponse>()
        assertTrue(lyricsCheckWithLyrics.success)

        val responseWithoutLyrics = client.get("/api/tracks/${trackWithoutLyrics.id.value}/lyrics/check")
        assertTrue(responseWithoutLyrics.status.isSuccess())

        val lyricsCheckWithoutLyrics = responseWithoutLyrics.body<LyricsCheckResponse>()
        assertFalse(lyricsCheckWithoutLyrics.success)
    }

    @Test
    fun testCheckTrackHasLyrics_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}/lyrics/check") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Download/Stream/Cover Permission Tests

    @Test
    fun testDownloadTrack_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.DOWNLOAD_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}/download") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testStreamTrack_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.STREAM_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}/stream") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    @Test
    fun testGetTrackCover_NoPermission() = testApp(false) { client ->
        val (_, token) = UserTestUtils.createUserAndSessionWithPermissions(
            username = "testuser",
            password = "noperms",
            PermissionType.VIEW_TRACKS to false
        )

        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/${track.id.value}/cover") {
            bearerAuth(token)
        }

        assertEquals(403, response.status.value)
    }

    // endregion
}
