package utils

import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.repos.FavoriteRepo
import de.mickkc.vibin.widgets.WidgetContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

object WidgetTestUtils {

    suspend fun setupFavoriteTracks(userId: Long) {
        val track1 = TrackTestUtils.createTrack("Track 1", "Album 1", "Artist 1")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album 2", "Artist 2", coverChecksum = "abc123")
        FavoriteRepo.addFavorite(userId, FavoriteType.TRACK, track1.id.value, 1)
        FavoriteRepo.addFavorite(userId, FavoriteType.TRACK, track2.id.value, 2)
    }

    suspend fun setupFavoriteAlbums(userId: Long) {
        val album1 = AlbumTestUtils.createAlbum("Album 1", "Description", 2020)
        val album2 = AlbumTestUtils.createAlbum("Album 2", null, 2021)
        AlbumTestUtils.addCoverToAlbum(album2, "abc123")
        FavoriteRepo.addFavorite(userId, FavoriteType.ALBUM, album1.id.value, 1)
        FavoriteRepo.addFavorite(userId, FavoriteType.ALBUM, album2.id.value, 2)
    }

    suspend fun setupFavoriteArtists(userId: Long) {
        val artist1 = ArtistTestUtils.createArtist("Artist 1", "Description")
        val artist2 = ArtistTestUtils.createArtist("Artist 2", "")
        ArtistTestUtils.addCoverToArtist(artist2, "abc123")
        FavoriteRepo.addFavorite(userId, FavoriteType.ARTIST, artist1.id.value, 1)
        FavoriteRepo.addFavorite(userId, FavoriteType.ARTIST, artist2.id.value, 2)
    }

    fun createContext(
        widgetId: String = "test-widget",
        userId: Long = 1,
        fgColor: Int = 0xFFFFFF,
        bgColor: Int = 0x000000,
        accentColor: Int = 0xFF0000,
        language: String = "en",
        external: Boolean = false
    ) = WidgetContext(
        widgetId = widgetId,
        userId = userId,
        foregroundColor = fgColor,
        backgroundColor = bgColor,
        accentColor = accentColor,
        language = language,
        external = external
    )

    fun parseHtml(html: String): Document {
        return Jsoup.parse(html)
    }

    data class TestItemCheck(
        val title: String? = null,
        val subtitle: String? = null,
        val place: String? = null
    )

    fun testItems(
        html: String,
        vararg checks: TestItemCheck
    ) {
        val doc = parseHtml(html)
        val list = doc.selectFirst(".items")

        assertNotNull(list)
        assertEquals(checks.size, list.childNodeSize())

        checks.forEachIndexed { idx, check ->
            val item = list.child(idx)

            if (check.title != null) {
                assertEquals(check.title, item.selectFirst(".item-info > h3")!!.text())
            }
            if (check.subtitle != null) {
                assertEquals(check.subtitle, item.selectFirst(".item-subtitle")!!.text())
            }

            val placeElement = item.selectFirst(".item-place")
            if (check.place != null) {
                assertNotNull(placeElement)
                assertEquals(check.place, placeElement.text())
            }
            else {
                assertNull(placeElement)
            }
        }
    }
}