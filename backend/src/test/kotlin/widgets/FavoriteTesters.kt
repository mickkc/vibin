package widgets

import org.jsoup.nodes.Element
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object FavoriteTesters {

    fun testFavoriteArtists(items: Element) {
        val firstItem = items.child(0)
        assertTrue(firstItem.hasClass("favorite-item"))
        assertTrue(firstItem.select("img.item-cover").attr("src").startsWith("/api/widgets/images/default-artist?"))
        assertEquals("Artist 1", firstItem.select(".item-info h3").text())
        assertEquals("Description", firstItem.select(".item-subtitle").text())
        assertEquals("🥇", firstItem.select(".favorite-place").text())

        val secondItem = items.child(1)
        assertTrue(secondItem.hasClass("favorite-item"))
        assertTrue(secondItem.select("img.item-cover").attr("src").startsWith("/api/widgets/images/abc123?"))
        assertEquals("Artist 2", secondItem.select(".item-info h3").text())
        assertEquals("Artist", secondItem.select(".item-subtitle").text())
        assertEquals("🥈", secondItem.select(".favorite-place").text())

        val thirdItem = items.child(2)
        assertTrue(thirdItem.hasClass("favorite-item"))
        assertEquals("background-color: #FF000033;", thirdItem.select("div.item-cover").single().attr("style"))
        assertEquals("", thirdItem.select(".item-info h3").text())
        assertEquals("", thirdItem.select(".item-subtitle").text())
        assertEquals("🥉", thirdItem.select(".favorite-place").text())
    }

    fun testFavoriteAlbums(items: Element) {
        val firstItem = items.child(0)
        assertTrue(firstItem.hasClass("favorite-item"))
        assertTrue(firstItem.select("img.item-cover").attr("src").startsWith("/api/widgets/images/default-album?"))
        assertEquals("Album 1", firstItem.select(".item-info h3").text())
        assertEquals("Description", firstItem.select(".item-subtitle").text())
        assertEquals("🥇", firstItem.select(".favorite-place").text())

        val secondItem = items.child(1)
        assertTrue(secondItem.hasClass("favorite-item"))
        assertTrue(secondItem.select("img.item-cover").attr("src").startsWith("/api/widgets/images/abc123?"))
        assertEquals("Album 2", secondItem.select(".item-info h3").text())
        assertEquals("2021", secondItem.select(".item-subtitle").text())
        assertEquals("🥈", secondItem.select(".favorite-place").text())

        val thirdItem = items.child(2)
        assertTrue(thirdItem.hasClass("favorite-item"))
        assertEquals("background-color: #FF000033;", thirdItem.select("div.item-cover").single().attr("style"))
        assertEquals("", thirdItem.select(".item-info h3").text())
        assertEquals("", thirdItem.select(".item-subtitle").text())
        assertEquals("🥉", thirdItem.select(".favorite-place").text())
    }

    fun testFavoriteTracks(items: Element) {
        val firstItem = items.child(0)
        assertTrue(firstItem.hasClass("favorite-item"))
        assertTrue(firstItem.select("img.item-cover").attr("src").startsWith("/api/widgets/images/default-track?"))
        assertEquals("Track 1", firstItem.select(".item-info h3").text())
        assertEquals("Artist 1", firstItem.select(".item-subtitle").text())
        assertEquals("🥇", firstItem.select(".favorite-place").text())

        val secondItem = items.child(1)
        assertTrue(secondItem.hasClass("favorite-item"))
        assertTrue(secondItem.select("img.item-cover").attr("src").startsWith("/api/widgets/images/abc123?"))
        assertEquals("Track 2", secondItem.select(".item-info h3").text())
        assertEquals("Artist 2", secondItem.select(".item-subtitle").text())
        assertEquals("🥈", secondItem.select(".favorite-place").text())

        val thirdItem = items.child(2)
        assertTrue(thirdItem.hasClass("favorite-item"))
        assertEquals("background-color: #FF000033;", thirdItem.select("div.item-cover").single().attr("style"))
        assertEquals("", thirdItem.select(".item-info h3").text())
        assertEquals("", thirdItem.select(".item-subtitle").text())
        assertEquals("🥉", thirdItem.select(".favorite-place").text())
    }
}