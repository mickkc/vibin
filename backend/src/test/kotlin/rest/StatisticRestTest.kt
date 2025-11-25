package rest

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import utils.*
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.dto.StatisticsDto
import de.mickkc.vibin.dto.UserActivityDto
import de.mickkc.vibin.dto.playlists.PlaylistDto
import de.mickkc.vibin.dto.tags.TagDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.repos.SettingsRepo
import de.mickkc.vibin.settings.user.ShowActivitiesToOthers
import de.mickkc.vibin.utils.DateTimeUtils
import kotlinx.coroutines.delay
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class StatisticRestTest {

    @Serializable
    private data class SuccessResponse(
        val success: Boolean
    )

    suspend fun sleep() {
        delay(1.seconds)
    }

    // region Get Recent Tracks

    @Test
    fun testGetRecentTracks() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Track 1", "Album 1", "Artist 1")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album 2", "Artist 2")
        val track3 = TrackTestUtils.createTrack("Track 3", "Album 3", "Artist 3")

        // Record listens
        ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK)
        sleep()
        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK)
        sleep()
        ListenRepo.listenedTo(1, track3.id.value, ListenType.TRACK)

        val response = client.get("/api/stats/recent") {
            parameter("limit", 2)
        }

        assertTrue(response.status.isSuccess())

        val recentTracks = response.body<List<MinimalTrackDto>>()

        assertEquals(2, recentTracks.size)

        assertEquals("Track 3", recentTracks[0].title)
        assertEquals("Track 2", recentTracks[1].title)
    }

    @Test
    fun testGetRecentTracks_DefaultLimit() = testApp { client ->
        repeat(6) { i ->
            val track = TrackTestUtils.createTrack("Track $i", "Album", "Artist")
            ListenRepo.listenedTo(1, track.id.value, ListenType.TRACK)
            sleep()
        }

        val response = client.get("/api/stats/recent")

        assertTrue(response.status.isSuccess())

        val recentTracks = response.body<List<MinimalTrackDto>>()

        assertEquals(5, recentTracks.size)
    }

    @Test
    fun testGetRecentTracks_NoListens() = testApp { client ->
        val response = client.get("/api/stats/recent")

        assertTrue(response.status.isSuccess())

        val recentTracks = response.body<List<MinimalTrackDto>>()

        assertEquals(0, recentTracks.size)
    }

    // endregion

    // region Get Recent Non-Tracks

    @Test
    fun testGetRecentNonTracks() = testApp { client ->
        val artist = ArtistTestUtils.createArtist("Artist 1", "Description")
        val album = AlbumTestUtils.createAlbum("Album 1", "Description", 2020)

        ListenRepo.listenedTo(1, artist.id.value, ListenType.ARTIST)
        sleep()
        ListenRepo.listenedTo(1, album.id.value, ListenType.ALBUM)

        val response = client.get("/api/stats/recent/nontracks") {
            parameter("limit", 5)
        }

        assertTrue(response.status.isSuccess())

        val recentNonTracks = response.body<List<KeyValueDto>>()

        assertEquals(2, recentNonTracks.size)

        assertEquals("ALBUM", recentNonTracks[0].key)
        assertEquals("Album 1", (recentNonTracks[0].value as Map<*, *>)["title"])

        assertEquals("ARTIST", recentNonTracks[1].key)
        assertEquals("Artist 1", (recentNonTracks[1].value as Map<*, *>)["name"])
    }

    @Test
    fun testGetRecentNonTracks_DefaultLimit() = testApp { client ->
        val response = client.get("/api/stats/recent/nontracks")

        assertTrue(response.status.isSuccess())

        val recentNonTracks = response.body<List<Map<String, Any>>>()

        // Should succeed even with no data
        assertTrue(recentNonTracks.isEmpty())
    }

    // endregion

    // region Get Top Items

    @Test
    fun testGetTopTracks() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")
        val track3 = TrackTestUtils.createTrack("Track 3", "Album", "Artist")

        // Track 2 listened 3 times, Track 1 listened 2 times, Track 3 listened 1 time
        repeat(3) { ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK, false) }
        repeat(2) { ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK, false) }
        ListenRepo.listenedTo(1, track3.id.value, ListenType.TRACK)

        val response = client.get("/api/stats/tracks/top3")

        assertTrue(response.status.isSuccess())

        val topTracks = response.body<List<MinimalTrackDto>>()

        assertEquals(3, topTracks.size)
        assertEquals("Track 2", topTracks[0].title) // Most listened
        assertEquals("Track 1", topTracks[1].title)
        assertEquals("Track 3", topTracks[2].title)
    }

    @Test
    fun testGetTopTracks_WithSinceParameter() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK)

        val now = System.currentTimeMillis()

        val response = client.get("/api/stats/tracks/top2") {
            parameter("since", now)
        }

        assertTrue(response.status.isSuccess())

        val topTracks = response.body<List<MinimalTrackDto>>()

        // All listens should be filtered out since they happened before 'now'
        assertEquals(0, topTracks.size)
    }

    @Test
    fun testGetTopArtists() = testApp { client ->

        // Create tracks for artists
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist 1")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist 2")

        // Listen to track 1 more
        repeat(3) { ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK, false) }
        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK)

        val response = client.get("/api/stats/artists/top2")

        assertTrue(response.status.isSuccess())

        val topArtists = response.body<List<Map<String, Any>>>()

        assertEquals(2, topArtists.size)
    }

    @Test
    fun testGetTopAlbums() = testApp { client ->
        val album1 = AlbumTestUtils.createAlbum("Album 1", "Description", 2020)
        val album2 = AlbumTestUtils.createAlbum("Album 2", "Description", 2021)

        ListenRepo.listenedTo(1, album1.id.value, ListenType.ALBUM)
        ListenRepo.listenedTo(1, album1.id.value, ListenType.ALBUM)
        ListenRepo.listenedTo(1, album2.id.value, ListenType.ALBUM)

        val response = client.get("/api/stats/albums/top2")

        assertTrue(response.status.isSuccess())

        val topAlbums = response.body<List<Map<String, Any>>>()

        assertEquals(2, topAlbums.size)
    }

    @Test
    fun testGetTopTags() = testApp { client ->

        // Create tracks with tags
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist", tags = listOf("Tag 1", "Tag 2"))
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist", tags = listOf("Tag 2"))

        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK, false)

        val response = client.get("/api/stats/tags/top2")

        assertTrue(response.status.isSuccess())

        val topTags = response.body<List<TagDto>>()

        // May be empty if no track-tag associations exist
        assertFalse(topTags.isEmpty())

        assertEquals(2, topTags.size)

        assertEquals("Tag 2", topTags[0].name)
        assertEquals("Tag 1", topTags[1].name)
    }

    @Test
    fun testGetTopPlaylists() = testApp { client ->

        val playlist1 = PlaylistTestUtils.createPlaylist("Playlist 1", true, 1)
        val playlist2 = PlaylistTestUtils.createPlaylist("Playlist 2", true, 1)

        ListenRepo.listenedTo(1, playlist2.id.value, ListenType.PLAYLIST)
        ListenRepo.listenedTo(1, playlist1.id.value, ListenType.PLAYLIST)
        ListenRepo.listenedTo(1, playlist2.id.value, ListenType.PLAYLIST)

        val response = client.get("/api/stats/playlists/top2")

        assertTrue(response.status.isSuccess())

        val topPlaylists = response.body<List<PlaylistDto>>()

        assertEquals(2, topPlaylists.size)

        assertEquals("Playlist 2", topPlaylists[0].name)
        assertEquals("Playlist 1", topPlaylists[1].name)
    }

    @Test
    fun testGetTopNonTracks() = testApp { client ->

        val playlist = PlaylistTestUtils.createPlaylist("Playlist", true, 1)
        val artist = ArtistTestUtils.createArtist("Artist", "Description")
        val album = AlbumTestUtils.createAlbum("Album", "Description", 2020)

        ListenRepo.listenedTo(1, playlist.id.value, ListenType.PLAYLIST)
        repeat(2) { ListenRepo.listenedTo(1, artist.id.value, ListenType.ARTIST) }
        repeat(3) { ListenRepo.listenedTo(1, album.id.value, ListenType.ALBUM) }

        val response = client.get("/api/stats/nontracks/top5")

        assertTrue(response.status.isSuccess())

        val topNonTracks = response.body<List<KeyValueDto>>()

        val albumResult = topNonTracks.first()
        assertEquals("ALBUM", albumResult.key)
        assertEquals("Album", (albumResult.value as Map<*, *>)["title"])

        val artistResult = topNonTracks[1]
        assertEquals("ARTIST", artistResult.key)
        assertEquals("Artist", (artistResult.value as Map<*, *>)["name"])

        val playlistResult = topNonTracks[2]
        assertEquals("PLAYLIST", playlistResult.key)
        assertEquals("Playlist", (playlistResult.value as Map<*, *>)["name"])
    }

    @Test
    fun testGetTopGlobalNonTracks() = testApp { client ->

        val user = UserTestUtils.createTestUser("user", "password")

        val playlist = PlaylistTestUtils.createPlaylist("Playlist", true, 1)
        val artist = ArtistTestUtils.createArtist("Artist", "Description")
        val album = AlbumTestUtils.createAlbum("Album", "Description", 2020)

        ListenRepo.listenedTo(user.id.value, playlist.id.value, ListenType.PLAYLIST)
        repeat(2) { ListenRepo.listenedTo(user.id.value, artist.id.value, ListenType.ARTIST) }
        repeat(3) { ListenRepo.listenedTo(user.id.value, album.id.value, ListenType.ALBUM) }

        val response = client.get("/api/stats/global_nontracks/top5")

        assertTrue(response.status.isSuccess())

        val topNonTracks = response.body<List<KeyValueDto>>()

        val albumResult = topNonTracks.first()
        assertEquals("ALBUM", albumResult.key)
        assertEquals("Album", (albumResult.value as Map<*, *>)["title"])

        val artistResult = topNonTracks[1]
        assertEquals("ARTIST", artistResult.key)
        assertEquals("Artist", (artistResult.value as Map<*, *>)["name"])

        val playlistResult = topNonTracks[2]
        assertEquals("PLAYLIST", playlistResult.key)
        assertEquals("Playlist", (playlistResult.value as Map<*, *>)["name"])
    }

    @Test
    fun testGetTopItems_InvalidType() = testApp { client ->
        val response = client.get("/api/stats/invalid/top5")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testGetTopItems_InvalidNum() = testApp { client ->
        val response = client.get("/api/stats/tracks/top0")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testGetTopItems_NegativeNum() = testApp { client ->
        val response = client.get("/api/stats/tracks/top-1")

        assertEquals(400, response.status.value)
    }

    // endregion

    // region Post Listen

    @Test
    fun testPostListen_Track() = testApp { client ->
        val track = TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.post("/api/stats/listen/TRACK/${track.id.value}")

        assertTrue(response.status.isSuccess())

        val successResponse = response.body<SuccessResponse>()

        assertTrue(successResponse.success)

        // Verify the listen was recorded
        val recentResponse = client.get("/api/stats/recent")
        val recentTracks = recentResponse.body<List<MinimalTrackDto>>()

        assertEquals(1, recentTracks.size)
        assertEquals("Track", recentTracks[0].title)
    }

    @Test
    fun testPostListen_Album() = testApp { client ->
        val album = AlbumTestUtils.createAlbum("Album", "Description", 2020)

        val response = client.post("/api/stats/listen/ALBUM/${album.id.value}")

        assertTrue(response.status.isSuccess())

        val successResponse = response.body<SuccessResponse>()

        assertTrue(successResponse.success)
    }

    @Test
    fun testPostListen_Artist() = testApp { client ->
        val artist = ArtistTestUtils.createArtist("Artist", "Description")

        val response = client.post("/api/stats/listen/ARTIST/${artist.id.value}")

        assertTrue(response.status.isSuccess())

        val successResponse = response.body<SuccessResponse>()

        assertTrue(successResponse.success)
    }

    @Test
    fun testPostListen_InvalidType() = testApp { client ->
        val response = client.post("/api/stats/listen/INVALID/1")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testPostListen_MissingEntityId() = testApp { client ->
        val response = client.post("/api/stats/listen/TRACK/")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testPostListen_InvalidEntityId() = testApp { client ->
        val response = client.post("/api/stats/listen/TRACK/invalid")

        assertEquals(400, response.status.value)
    }

    // endregion

    // region Get User Activity

    @Test
    fun testGetUserActivity() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist 1")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist 2")

        // Record some listens for user 1
        repeat(3) { ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK, false) }
        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK)

        val response = client.get("/api/stats/users/1/activity") {
            parameter("limit", 5)
        }

        assertTrue(response.status.isSuccess())

        val activity = response.body<UserActivityDto>()

        assertNotNull(activity.recentTracks)
        assertNotNull(activity.topTracks)
        assertNotNull(activity.topArtists)

        assertTrue(activity.recentTracks.isNotEmpty())
        assertTrue(activity.topTracks.isNotEmpty())
        assertTrue(activity.topArtists.isNotEmpty())
    }

    @Test
    fun testGetUserActivity_WithSinceParameter() = testApp { client ->
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        ListenRepo.listenedTo(1, track1.id.value, ListenType.TRACK)
        sleep()
        val since = DateTimeUtils.now()
        ListenRepo.listenedTo(1, track2.id.value, ListenType.TRACK)

        val response = client.get("/api/stats/users/1/activity") {
            parameter("since", since)
            parameter("limit", 10)
        }

        assertTrue(response.status.isSuccess())

        val activity = response.body<UserActivityDto>()

        assertNotNull(activity.recentTracks)
        assertNotNull(activity.topTracks)
        assertNotNull(activity.topArtists)

        assertEquals(1, activity.recentTracks.size)
        assertEquals(track2.id.value, activity.recentTracks[0].id)

        assertEquals(1, activity.topTracks.size)
        assertEquals(track2.id.value, activity.topTracks[0].id)
    }

    @Test
    fun testGetUserActivity_InvalidLimit() = testApp { client ->
        val response = client.get("/api/stats/users/1/activity") {
            parameter("limit", 0)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testGetUserActivity_NegativeLimit() = testApp { client ->
        val response = client.get("/api/stats/users/1/activity") {
            parameter("limit", -1)
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testGetUserActivity_MissingUserId() = testApp { client ->
        val response = client.get("/api/stats/users//activity")

        assertEquals(404, response.status.value)
    }

    @Test
    fun testGetUserActivity_InvalidUserId() = testApp { client ->
        val response = client.get("/api/stats/users/invalid/activity")

        assertEquals(400, response.status.value)
    }

    @Test
    fun testGetUserActivity_PrivacySettings() = testApp(false) { client ->
        // Create two users
        val (_, token1) = UserTestUtils.createUserWithSession("user1", "password1")
        val (user2, _) = UserTestUtils.createUserWithSession("user2", "password2")

        SettingsRepo.updateUserSetting(ShowActivitiesToOthers, user2.id.value, "false")

        // Try to access user2's activity as user1
        // This should be forbidden if user2 has privacy settings enabled
        val response = client.get("/api/stats/users/${user2.id.value}/activity") {
            bearerAuth(token1)
        }

        assertEquals(403, response.status.value)
    }

    // endregion

    // region Get Global Statistics

    @Test
    fun testGetGlobalStatistics() = testApp { client ->
        // Create some test data
        TrackTestUtils.createTrack("Track 1", "Album 1", "Artist 1")
        TrackTestUtils.createTrack("Track 2", "Album 2", "Artist 2")
        AlbumTestUtils.createAlbum("Album 3", "Description", 2020)
        ArtistTestUtils.createArtist("Artist 3", "Description")

        val response = client.get("/api/stats/global")

        assertTrue(response.status.isSuccess())

        val stats = response.body<StatisticsDto>()

        assertNotNull(stats.totalTracks)
        assertNotNull(stats.totalTrackDuration)
        assertNotNull(stats.totalAlbums)
        assertNotNull(stats.totalArtists)
        assertNotNull(stats.totalPlaylists)
        assertNotNull(stats.totalUsers)
        assertNotNull(stats.totalPlays)

        assertEquals(2, stats.totalTracks)
        assertEquals(3, stats.totalAlbums) // 2 from tracks + 1 standalone
        assertEquals(3, stats.totalArtists) // 2 from tracks + 1 standalone
    }

    @Test
    fun testGetGlobalStatistics_EmptyDatabase() = testApp { client ->
        val response = client.get("/api/stats/global")

        assertTrue(response.status.isSuccess())

        val stats = response.body<StatisticsDto>()

        assertNotNull(stats.totalTracks)
        assertNotNull(stats.totalTrackDuration)
        assertNotNull(stats.totalAlbums)
        assertNotNull(stats.totalArtists)
        assertNotNull(stats.totalPlaylists)
        assertNotNull(stats.totalUsers)
        assertNotNull(stats.totalPlays)
    }

    // endregion
}