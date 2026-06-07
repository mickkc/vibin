package rest

import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.dto.PaginatedDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackSortingCriteria
import de.mickkc.vibin.repos.TrackRepo
import de.mickkc.vibin.repos.UserRepo
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.transactions.transaction
import utils.ListenTestUtils
import utils.TrackTestUtils
import utils.initTestDb
import utils.testApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackSortingRestTest {

    private suspend fun getSortedTitles(
        client: HttpClient,
        sort: String? = null,
        dir: String? = null,
        endpoint: String = "/api/tracks",
        query: String? = null
    ): List<String> {
        val response = client.get(endpoint) {
            parameter("pageSize", 100)
            sort?.let { parameter("sort", it) }
            dir?.let { parameter("dir", it) }
            query?.let { parameter("query", it) }
        }
        assertTrue(response.status.isSuccess())
        return response.body<PaginatedDto<MinimalTrackDto>>().items.map { it.title }
    }

    // region Sort by Title

    @Test
    fun testSortByTitle_Ascending() = testApp { client ->
        TrackTestUtils.createTrack("Track C", "Album", "Artist")
        TrackTestUtils.createTrack("Track A", "Album", "Artist")
        TrackTestUtils.createTrack("Track B", "Album", "Artist")

        assertEquals(listOf("Track A", "Track B", "Track C"), getSortedTitles(client, sort = "TITLE", dir = "ASC"))
    }

    @Test
    fun testSortByTitle_Descending() = testApp { client ->
        TrackTestUtils.createTrack("Track B", "Album", "Artist")
        TrackTestUtils.createTrack("Track C", "Album", "Artist")
        TrackTestUtils.createTrack("Track A", "Album", "Artist")

        assertEquals(listOf("Track C", "Track B", "Track A"), getSortedTitles(client, sort = "TITLE", dir = "DESC"))
    }

    // endregion

    // region Sort by Release Year

    @Test
    fun testSortByReleaseYear() = testApp { client ->
        TrackTestUtils.createTrack("New", "Album", "Artist", year = 2020)
        TrackTestUtils.createTrack("Old", "Album", "Artist", year = 1990)
        TrackTestUtils.createTrack("Mid", "Album", "Artist", year = 2010)
        TrackTestUtils.createTrack("Unknown Year", "Album", "Artist", year = null)

        assertEquals(
            listOf("Old", "Mid", "New", "Unknown Year"),
            getSortedTitles(client, sort = "RELEASE_YEAR", dir = "ASC")
        )

        // Tracks without a year are placed last regardless of direction
        assertEquals(
            listOf("New", "Mid", "Old", "Unknown Year"),
            getSortedTitles(client, sort = "RELEASE_YEAR", dir = "DESC")
        )
    }

    // endregion

    // region Sort by Audio Quality

    @Test
    fun testSortByAudioQuality() = testApp { client ->
        TrackTestUtils.createTrack("High Quality", "Album", "Artist", bitrate = 320)
        TrackTestUtils.createTrack("Low Quality", "Album", "Artist", bitrate = 128)
        TrackTestUtils.createTrack("Mid Quality", "Album", "Artist", bitrate = 256)

        assertEquals(
            listOf("Low Quality", "Mid Quality", "High Quality"),
            getSortedTitles(client, sort = "AUDIO_QUALITY", dir = "ASC")
        )
        assertEquals(
            listOf("High Quality", "Mid Quality", "Low Quality"),
            getSortedTitles(client, sort = "AUDIO_QUALITY", dir = "DESC")
        )
    }

    // endregion

    // region Sort by Audio Duration

    @Test
    fun testSortByAudioDuration() = testApp { client ->
        TrackTestUtils.createTrack("Long Track", "Album", "Artist", duration = 360_000)
        TrackTestUtils.createTrack("Short Track", "Album", "Artist", duration = 120_000)
        TrackTestUtils.createTrack("Medium Track", "Album", "Artist", duration = 240_000)

        assertEquals(
            listOf("Short Track", "Medium Track", "Long Track"),
            getSortedTitles(client, sort = "AUDIO_DURATION", dir = "ASC")
        )
        assertEquals(
            listOf("Long Track", "Medium Track", "Short Track"),
            getSortedTitles(client, sort = "AUDIO_DURATION", dir = "DESC")
        )
    }

    // endregion

    // region Sort by Date Added

    @Test
    fun testSortByDateAdded() = testApp { client ->
        val first = TrackTestUtils.createTrack("First Added", "Album", "Artist")
        val second = TrackTestUtils.createTrack("Second Added", "Album", "Artist")
        val third = TrackTestUtils.createTrack("Third Added", "Album", "Artist")

        transaction {
            first.createdAt = 1000
            second.createdAt = 2000
            third.createdAt = 3000
        }

        assertEquals(
            listOf("First Added", "Second Added", "Third Added"),
            getSortedTitles(client, sort = "DATE_ADDED", dir = "ASC")
        )
        assertEquals(
            listOf("Third Added", "Second Added", "First Added"),
            getSortedTitles(client, sort = "DATE_ADDED", dir = "DESC")
        )
    }

    // endregion

    // region Sort by Date Modified

    @Test
    fun testSortByDateModified() = testApp { client ->
        val neverModified = TrackTestUtils.createTrack("Never Modified", "Album", "Artist")
        val recentlyModified = TrackTestUtils.createTrack("Recently Modified", "Album", "Artist")
        val longAgoModified = TrackTestUtils.createTrack("Long Ago Modified", "Album", "Artist")

        transaction {
            neverModified.createdAt = 2500
            neverModified.updatedAt = null

            recentlyModified.createdAt = 1500
            recentlyModified.updatedAt = 3000

            longAgoModified.createdAt = 1500
            longAgoModified.updatedAt = 2000
        }

        assertEquals(
            listOf("Long Ago Modified", "Never Modified", "Recently Modified"),
            getSortedTitles(client, sort = "DATE_MODIFIED", dir = "ASC")
        )
        assertEquals(
            listOf("Recently Modified", "Never Modified", "Long Ago Modified"),
            getSortedTitles(client, sort = "DATE_MODIFIED", dir = "DESC")
        )
    }

    // endregion

    // region Sort by Listen Duration

    @Test
    fun testSortByListenDuration() = testApp { client ->
        val user = UserRepo.getById(1)!!

        val mostListened = TrackTestUtils.createTrack("Most Listened", "Album", "Artist")
        val leastListened = TrackTestUtils.createTrack("Least Listened", "Album", "Artist")
        val midListened = TrackTestUtils.createTrack("Mid Listened", "Album", "Artist")
        val neverListened = TrackTestUtils.createTrack("Never Listened", "Album", "Artist")

        ListenTestUtils.listenedToTrack(user, mostListened, 300)
        ListenTestUtils.listenedToTrack(user, leastListened, 100)
        ListenTestUtils.listenedToTrack(user, midListened, 200)

        assertEquals(
            listOf("Never Listened", "Least Listened", "Mid Listened", "Most Listened"),
            getSortedTitles(client, sort = "LISTEN_DURATION", dir = "ASC")
        )
        assertEquals(
            listOf("Most Listened", "Mid Listened", "Least Listened", "Never Listened"),
            getSortedTitles(client, sort = "LISTEN_DURATION", dir = "DESC")
        )
    }

    @Test
    fun testSortByListenDuration_SumsMultipleListens() = testApp { client ->
        val user = UserRepo.getById(1)!!

        val higherTotal = TrackTestUtils.createTrack("Higher Total", "Album", "Artist")
        val lowerTotal = TrackTestUtils.createTrack("Lower Total", "Album", "Artist")

        ListenTestUtils.listenedToTrack(user, higherTotal, 50)
        ListenTestUtils.listenedToTrack(user, higherTotal, 50)
        ListenTestUtils.listenedToTrack(user, higherTotal, 50)
        ListenTestUtils.listenedToTrack(user, lowerTotal, 100)

        assertEquals(
            listOf("Lower Total", "Higher Total"),
            getSortedTitles(client, sort = "LISTEN_DURATION", dir = "ASC")
        )
    }

    // endregion

    // region Sort by Last Listened To

    @Test
    fun testSortByLastListenedTo() = testApp { client ->
        val user = UserRepo.getById(1)!!

        val listenedRecently = TrackTestUtils.createTrack("Listened Recently", "Album", "Artist")
        val listenedLongAgo = TrackTestUtils.createTrack("Listened Long Ago", "Album", "Artist")
        val listenedInBetween = TrackTestUtils.createTrack("Listened In Between", "Album", "Artist")
        val neverListened = TrackTestUtils.createTrack("Never Listened", "Album", "Artist")

        ListenTestUtils.listenedToTrack(user, listenedRecently, 60, at = 3000)
        ListenTestUtils.listenedToTrack(user, listenedLongAgo, 60, at = 1000)
        ListenTestUtils.listenedToTrack(user, listenedInBetween, 60, at = 2000)

        assertEquals(
            listOf("Listened Long Ago", "Listened In Between", "Listened Recently", "Never Listened"),
            getSortedTitles(client, sort = "LAST_LISTENED_TO", dir = "ASC")
        )
        assertEquals(
            listOf("Listened Recently", "Listened In Between", "Listened Long Ago", "Never Listened"),
            getSortedTitles(client, sort = "LAST_LISTENED_TO", dir = "DESC")
        )
    }

    // endregion

    // region Sort Randomly

    @Test
    fun testSortRandomly() = testApp { client ->
        repeat(10) { i ->
            TrackTestUtils.createTrack("Track $i", "Album", "Artist")
        }

        val response = client.get("/api/tracks") {
            parameter("pageSize", 100)
            parameter("sort", "RANDOM")
        }

        assertTrue(response.status.isSuccess())

        val tracks = response.body<PaginatedDto<MinimalTrackDto>>()
        assertEquals(10, tracks.total)
        assertEquals(10, tracks.items.size)
    }

    // endregion

    // region Invalid Parameters

    @Test
    fun testSort_InvalidCriteria() = testApp { client ->
        TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks") {
            parameter("sort", "NOT_A_REAL_CRITERIA")
        }

        assertEquals(400, response.status.value)
    }

    @Test
    fun testSort_InvalidDirection() = testApp { client ->
        TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks") {
            parameter("dir", "SIDEWAYS")
        }

        assertEquals(400, response.status.value)
    }

    // endregion

    // region Sorting via Search

    @Test
    fun testSearchTracks_SortByTitleDescending() = testApp { client ->
        TrackTestUtils.createTrack("Rock Track 1", "Album", "Artist")
        TrackTestUtils.createTrack("Rock Track 2", "Album", "Artist")
        TrackTestUtils.createTrack("Rock Track 3", "Album", "Artist")
        TrackTestUtils.createTrack("Jazz Track 1", "Album", "Artist")

        assertEquals(
            listOf("Rock Track 3", "Rock Track 2", "Rock Track 1"),
            getSortedTitles(client, sort = "TITLE", dir = "DESC", endpoint = "/api/tracks/search", query = "Rock")
        )
    }

    @Test
    fun testSearchTracks_InvalidSortCriteria() = testApp { client ->
        TrackTestUtils.createTrack("Track", "Album", "Artist")

        val response = client.get("/api/tracks/search") {
            parameter("query", "Track")
            parameter("sort", "NOT_A_REAL_CRITERIA")
        }

        assertEquals(400, response.status.value)
    }

    // endregion

    // region Sorting Expression Fallback (no user)

    @Test
    fun testGetSortingExpr_ListenDuration_NoUser_FallsBackToTitle() {
        initTestDb()
        transaction {
            assertEquals(TrackTable.title, TrackRepo.getSortingExpr(TrackSortingCriteria.LISTEN_DURATION, null))
        }
    }

    @Test
    fun testGetSortingExpr_LastListenedTo_NoUser_FallsBackToTitle() {
        initTestDb()
        transaction {
            assertEquals(TrackTable.title, TrackRepo.getSortingExpr(TrackSortingCriteria.LAST_LISTENED_TO, null))
        }
    }

    // endregion
}
