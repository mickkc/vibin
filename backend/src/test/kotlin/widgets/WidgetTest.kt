package widgets

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.dto.users.UserEditDto
import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.WidgetBuilder
import de.mickkc.vibin.widgets.WidgetContext
import de.mickkc.vibin.widgets.WidgetType
import de.mickkc.vibin.widgets.WidgetUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.*
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WidgetTest {

    private fun createContext(
        userId: Long = 1,
        fgColor: Int = 0xFFFFFF,
        bgColor: Int = 0x000000,
        accentColor: Int = 0xFF0000,
        language: String = "en"
    ) = WidgetContext(
        userId = userId,
        foregroundColor = fgColor,
        backgroundColor = bgColor,
        accentColor = accentColor,
        language = language
    )

    private fun parseHtml(html: String): Document {
        return Jsoup.parse(html)
    }

    // region WidgetUtils Tests

    @Test
    fun testColorToHex() {
        assertEquals("#FFFFFF", WidgetUtils.colorToHex(0xFFFFFF))
        assertEquals("#000000", WidgetUtils.colorToHex(0x000000))
        assertEquals("#FF0000", WidgetUtils.colorToHex(0xFF0000))
        assertEquals("#00FF00", WidgetUtils.colorToHex(0x00FF00))
        assertEquals("#0000FF", WidgetUtils.colorToHex(0x0000FF))
        assertEquals("#123456", WidgetUtils.colorToHex(0x123456))
    }

    @Test
    fun testColorFromHex_Valid() {
        assertEquals(0xFF_FFFFFF.toInt(), WidgetUtils.colorFromHex("FFFFFF"))
        assertEquals(0xFF_000000.toInt(), WidgetUtils.colorFromHex("000000"))
        assertEquals(0xFF_FF0000.toInt(), WidgetUtils.colorFromHex("FF0000"))
        assertEquals(0xFF_00FF00.toInt(), WidgetUtils.colorFromHex("00FF00"))
        assertEquals(0xFF_0000FF.toInt(), WidgetUtils.colorFromHex("0000FF"))
        assertEquals(0xFF_123456.toInt(), WidgetUtils.colorFromHex("123456"))
    }

    @Test
    fun testColorFromHex_CaseInsensitive() {
        assertEquals(
            WidgetUtils.colorFromHex("ABCDEF"),
            WidgetUtils.colorFromHex("abcdef")
        )
        assertEquals(
            WidgetUtils.colorFromHex("FF00FF"),
            WidgetUtils.colorFromHex("ff00ff")
        )
    }

    @Test
    fun testColorFromHex_Invalid() {
        assertNull(WidgetUtils.colorFromHex("FFFFF")) // Too short
        assertNull(WidgetUtils.colorFromHex("FFFFFFF")) // Too long
        assertNull(WidgetUtils.colorFromHex("GGGGGG")) // Invalid characters
        assertNull(WidgetUtils.colorFromHex("FF FF FF")) // Spaces
        assertNull(WidgetUtils.colorFromHex("#FFFFFF")) // With hash
        assertNull(WidgetUtils.colorFromHex("")) // Empty
    }

    @Test
    fun testBlendColors() {

        val blended = WidgetUtils.blendColors(0xFFFFFF, 0x000000, 0.5f)
        assertEquals("#7F7F7F", WidgetUtils.colorToHex(blended))

        val blend0 = WidgetUtils.blendColors(0xFF0000, 0x0000FF, 0f)
        assertEquals("#FF0000", WidgetUtils.colorToHex(blend0))

        val blend100 = WidgetUtils.blendColors(0xFF0000, 0x0000FF, 1f)
        assertEquals("#0000FF", WidgetUtils.colorToHex(blend100))
    }

    @Test
    fun testGetImageUrl() {
        val url = WidgetUtils.getImageUrl(null, "track", 192)
        assertNotNull(url)
        assertTrue(url.contains("default-track"))
    }

    // endregion

    // region WidgetBuilder Tests

    @Test
    fun testWidgetBuilder_BasicStructure() = testApp { _ ->
        val ctx = createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctx)

        val doc = parseHtml(html)

        // Check basic HTML structure
        assertNotNull(doc.select("html").first())
        assertNotNull(doc.select("head").first())
        assertNotNull(doc.select("body").first())

        // Check head elements
        assertNotNull(doc.select("title").first())
        assertEquals("Vibin' Widget", doc.select("title").text())

        // Check meta tags
        assertTrue(doc.select("meta[name=charset]").isNotEmpty())
        assertTrue(doc.select("meta[name=viewport]").isNotEmpty())

        // Check stylesheet link
        val styleLink = doc.select("link[rel=stylesheet]").first()
        assertNotNull(styleLink)
        assertEquals("/api/widgets/styles", styleLink.attr("href"))

        // Check language
        assertEquals("en", doc.select("html").attr("lang"))
    }

    @Test
    fun testWidgetBuilder_Colors() = testApp { _ ->
        val ctx = createContext(
            fgColor = 0xFFFFFF,
            bgColor = 0x123456
        )
        val html = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctx)

        val doc = parseHtml(html)

        val bodyStyle = doc.select("body").first()?.attr("style")
        assertNotNull(bodyStyle)
        assertTrue(bodyStyle.contains("background-color: #123456"))
        assertTrue(bodyStyle.contains("color: #FFFFFF"))
    }

    @Test
    fun testWidgetBuilder_Language() = testApp { _ ->
        val ctxEn = createContext(language = "en")
        val htmlEn = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctxEn)
        assertEquals("en", parseHtml(htmlEn).select("html").attr("lang"))

        val ctxDe = createContext(language = "de")
        val htmlDe = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctxDe)
        assertEquals("de", parseHtml(htmlDe).select("html").attr("lang"))
    }

    @Test
    fun testWidgetBuilder_MultipleWidgets() = testApp { _ ->
        val ctx = createContext()
        val html = WidgetBuilder.build(
            listOf(
                WidgetType.SERVER_STATS,
                WidgetType.USER
            ),
            ctx
        )

        val doc = parseHtml(html)

        // Should have multiple widget bodies
        val widgets = doc.select(".widget-body")
        assertEquals(2, widgets.size)
    }

    @Test
    fun testWidgetBuilder_PoweredByLink() = testApp { _ ->
        val ctx = createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctx)

        val doc = parseHtml(html)

        val poweredByLink = doc.select("a[href=https://github.com/mickkc/vibin]").first()
        assertNotNull(poweredByLink)
        assertEquals("Vibin'", poweredByLink.text())
        assertEquals("_blank", poweredByLink.attr("target"))
        assertEquals("noopener noreferrer", poweredByLink.attr("rel"))
    }

    @Test
    fun testWidgetBuilder_Interactive() = testApp { _ ->
        val ctx = createContext()
        val htmlInteractive = WidgetBuilder.build(listOf(WidgetType.USER), ctx, interactive = true)
        val htmlNonInteractive = WidgetBuilder.build(listOf(WidgetType.USER), ctx, interactive = false)

        val docInteractive = parseHtml(htmlInteractive)
        val docNonInteractive = parseHtml(htmlNonInteractive)

        // Interactive should have buttons/links
        assertTrue(docInteractive.select("a.btn").isNotEmpty())

        // Non-interactive should not have interactive elements
        assertTrue(docNonInteractive.select("a.btn").isEmpty())
    }

    // endregion

    // region ServerStatsWidget Tests

    @Test
    fun testServerStatsWidget_BasicContent() = testApp { _ ->
        // Create some test data
        TrackTestUtils.createTrack("Track 1", "Album 1", "Artist 1", duration = 1000)
        TrackTestUtils.createTrack("Track 2", "Album 2", "Artist 2", duration = 2000)
        AlbumTestUtils.createAlbum("Album 3", "Description", 2020)
        ArtistTestUtils.createArtist("Artist 3", "Description")
        PlaylistTestUtils.createPlaylist("Playlist 1", true, 1)
        PlaylistTestUtils.createPlaylist("Playlist 2", false, 1)

        ListenRepo.listenedTo(1, 1, ListenType.TRACK)

        val ctx = createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctx)

        val doc = parseHtml(html)

        val statCards = doc.select(".stat-card")
        assertEquals(7, statCards.size)

        statCards.forEach { card ->
            val value = card.child(0).text()
            val label = card.child(1).text()

            when (label) {
                "Tracks" -> assertEquals("2", value)
                "Total Duration" -> assertEquals("3s", value)
                "Albums" -> assertEquals("3", value)
                "Artists" -> assertEquals("3", value)
                "Playlists" -> assertEquals("2", value)
                "Users" -> assertEquals("1", value)
                "Play Count" -> assertEquals("1", value)
                else -> fail("Unexpected stat label: $label")
            }
        }
    }

    @Test
    fun testServerStatsWidget_DurationFormatting() = testApp { _ ->

        TrackTestUtils.createTrack(
            title = "Long Track",
            album = "Album",
            artists = "Artist",
            duration = (2.hours + 2.minutes + 5.seconds).inWholeMilliseconds
        )

        val ctx = createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctx)

        val doc = parseHtml(html)

        // Find the duration stat
        val bodyText = doc.text()
        // Should contain formatted duration with h/m/s
        assertTrue(bodyText.contains("2h 2m 5s"))
    }

    // endregion

    // region UserWidget Tests

    @Test
    fun testUserWidget_BasicContent() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        UserRepo.updateOrCreateUser(
            user.id.value,
            UserEditDto(
                username = user.username,
                password = null,
                email = null,
                isAdmin = false,
                isActive = true,
                displayName = "Test Display Name",
                profilePictureUrl = null,
                oldPassword = null,
                description = "Test description"
            )
        )

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.USER), ctx)

        val doc = parseHtml(html)

        // Check user container exists
        val userContainer = doc.select(".user-container").first()
        assertNotNull(userContainer)

        val img = userContainer.select("img").first()
        assertNotNull(img)
        assertTrue(img.attr("src").contains("default-user"))
    }

    @Test
    fun testUserWidget_Interactive() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        val ctx = createContext(userId = user.id.value)

        val htmlInteractive = WidgetBuilder.build(listOf(WidgetType.USER), ctx, interactive = true)
        val docInteractive = parseHtml(htmlInteractive)

        // Should have profile link button
        val profileLink = docInteractive.select("a.btn").first()
        assertNotNull(profileLink)
        assertTrue(profileLink.attr("href").contains("/web/users/${user.id.value}"))
        assertEquals("_blank", profileLink.attr("target"))
    }

    @Test
    fun testUserWidget_NonInteractive() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        val ctx = createContext(userId = user.id.value)

        val htmlNonInteractive = WidgetBuilder.build(listOf(WidgetType.USER), ctx, interactive = false)
        val docNonInteractive = parseHtml(htmlNonInteractive)

        // Should not have profile link button
        assertTrue(docNonInteractive.select("a.btn").isEmpty())
    }

    // endregion

    // region FavoriteTracksWidget Tests

    @Test
    fun testFavoriteTracksWidget_WithFavorites() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        WidgetTestUtils.setupFavoriteTracks(user.id.value)

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.FAVORITE_TRACKS), ctx)

        val doc = parseHtml(html)

        // Should contain favorite section
        val items = doc.select(".favorite-items").single()
        assertEquals(3, items.childrenSize())

        FavoriteTesters.testFavoriteTracks(items)
    }

    @Test
    fun testFavoriteTracksWidget_NoFavorites() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.FAVORITE_TRACKS), ctx)

        val doc = parseHtml(html)

        val items = doc.select(".favorite-items").single()
        assertEquals(3, items.childrenSize())

        items.children().withIndex().forEach { (index, element) ->
            assertTrue(element.hasClass("favorite-item"))
            assertEquals("background-color: #FF000033;", element.select("div.item-cover").single().attr("style"))
            assertEquals("", element.select(".item-info h3").text())
            assertEquals("", element.select(".item-subtitle").text())
            assertEquals(listOf("🥇", "🥈", "🥉")[index], element.select(".favorite-place").text())
        }
    }

    // endregion

    // region FavoriteAlbumsWidget Tests

    @Test
    fun testFavoriteAlbumsWidget_WithFavorites() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        WidgetTestUtils.setupFavoriteAlbums(user.id.value)

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.FAVORITE_ALBUMS), ctx)

        val doc = parseHtml(html)

        val items = doc.select(".favorite-items").single()
        assertEquals(3, items.childrenSize())

        FavoriteTesters.testFavoriteAlbums(items)
    }

    // endregion

    // region FavoriteArtistsWidget Tests

    @Test
    fun testFavoriteArtistsWidget_WithFavorites() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        WidgetTestUtils.setupFavoriteArtists(user.id.value)

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.FAVORITE_ARTISTS), ctx)

        val doc = parseHtml(html)

        val items = doc.select(".favorite-items").single()
        assertEquals(3, items.childrenSize())

        FavoriteTesters.testFavoriteArtists(items)
    }

    // endregion

    // region ActivityWidget Tests

    @Test
    fun testActivityWidget_WithActivity() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        // Record some listens
        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, at = DateTimeUtils.now() - 24 * 60 * 60)

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.SIMPLE_ACTIVITY), ctx)

        val doc = parseHtml(html)

        val grid = doc.select(".activity-grid").single()

        val daysToToday = DateTimeUtils.toLocalDate(DateTimeUtils.now()).dayOfYear
        val daysOffset = DateTimeUtils.toLocalDate(DateTimeUtils.startOfYear()).dayOfWeek.value - 1

        assertEquals(daysToToday + daysOffset, grid.childrenSize())

        for (day in 0..<daysOffset) {
            val cell = grid.child(day)
            assertEquals("", cell.text())
            assertEquals("", cell.attr("style"))
        }

        for (day in daysOffset..<(daysToToday + daysOffset - 2)) {
            val cell = grid.child(day)
            assertEquals("", cell.text())
            assertEquals("background-color: #190000;", cell.attr("style"))
        }

        val secondLastCell = grid.child(daysToToday + daysOffset - 2)
        assertEquals("", secondLastCell.text())
        assertEquals("background-color: #8C0000;", secondLastCell.attr("style"))

        val lastCell = grid.child(daysToToday + daysOffset - 1)
        assertEquals("", lastCell.text())
        assertEquals("background-color: #FF0000;", lastCell.attr("style"))
    }

    @Test
    fun testActivityWidget_Extended() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")
        val track1 = TrackTestUtils.createTrack("Track 1", "Album", "Artist")
        val track2 = TrackTestUtils.createTrack("Track 2", "Album", "Artist")

        // Record some listens
        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK)
        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, at = DateTimeUtils.now() - 1.days.inWholeSeconds)

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.ACTIVITY), ctx)

        val doc = parseHtml(html)

        val grid = doc.select(".activity-grid").single()

        val today = DateTimeUtils.toLocalDate(DateTimeUtils.now())
        val daysToToday = today.dayOfYear
        val daysOffset = DateTimeUtils.toLocalDate(DateTimeUtils.startOfYear()).dayOfWeek.value - 1

        assertEquals(daysToToday + daysOffset, grid.childrenSize())

        for (day in 0..<daysOffset) {
            val cell = grid.child(day)
            assertEquals("", cell.text())
            assertEquals("", cell.attr("style"))
        }

        for (day in daysOffset..<(daysToToday + daysOffset - 2)) {
            val cell = grid.child(day)
            assertEquals("", cell.text())
            assertEquals("background-color: #190000;", cell.attr("style"))
        }

        val secondLastCell = grid.child(daysToToday + daysOffset - 2)
        assertEquals("", secondLastCell.text())
        assertEquals("background-color: #8C0000;", secondLastCell.attr("style"))

        val lastCell = grid.child(daysToToday + daysOffset - 1)
        assertEquals("", lastCell.text())
        assertEquals("background-color: #FF0000;", lastCell.attr("style"))

        val stats = doc.select(".stats").single()
        val statCards = stats.select(".stat-card")
        assertEquals(4, statCards.size)

        val expectedStats = listOf(
            if (today.dayOfYear == 1) 2 else 3, // Year
            if (today.dayOfMonth == 1) 2 else 3, // Month
            if (today.dayOfWeek.value == 1) 2 else 3, // Week
            2 // Today
        )

        val expectedLabels = listOf(
            "Tracks this Year",
            "Tracks this Month",
            "Tracks this Week",
            "Tracks Today"
        )

        statCards.forEachIndexed { index, card ->
            assertEquals(expectedStats[index].toString(), card.child(0).single().text())
            assertEquals(expectedLabels[index], card.child(1).single().text())
        }
    }

    // endregion

    // region JoinedFavoritesWidget Tests

    @Test
    fun testJoinedFavoritesWidget() = testApp { _ ->
        val user = UserTestUtils.createTestUser("testuser", "password")

        // Tracks
        WidgetTestUtils.setupFavoriteTracks(user.id.value)

        // Albums
        WidgetTestUtils.setupFavoriteAlbums(user.id.value)

        // Artists
        WidgetTestUtils.setupFavoriteArtists(user.id.value)

        val ctx = createContext(userId = user.id.value)
        val html = WidgetBuilder.build(listOf(WidgetType.JOINED_FAVORITES), ctx)

        val doc = parseHtml(html)

        val sections = doc.select(".joined-favorites").single().children()
        assertEquals(3, sections.size)

        val tracksSection = sections[0]
        val trackItems = tracksSection.select(".favorite-items").single()
        assertEquals(3, trackItems.childrenSize())
        FavoriteTesters.testFavoriteTracks(trackItems)

        val albumsSection = sections[1]
        val albumItems = albumsSection.select(".favorite-items").single()
        assertEquals(3, albumItems.childrenSize())
        FavoriteTesters.testFavoriteAlbums(albumItems)

        val artistsSection = sections[2]
        val artistItems = artistsSection.select(".favorite-items").single()
        assertEquals(3, artistItems.childrenSize())
        FavoriteTesters.testFavoriteArtists(artistItems)
    }

    // endregion

    // region BaseWidget Tests

    @Test
    fun testBaseWidget_ColorProperties() = testApp { _ ->
        val ctx = createContext(
            fgColor = 0xABCDEF,
            bgColor = 0x123456,
            accentColor = 0xFFAABB
        )

        val html = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctx)
        val doc = parseHtml(html)

        val bodyStyle = doc.select("body").first()?.attr("style")
        assertNotNull(bodyStyle)
        assertTrue(bodyStyle.contains("#ABCDEF"))
        assertTrue(bodyStyle.contains("#123456"))
    }

    @Test
    fun testBaseWidget_Translation() = testApp { _ ->
        val ctxEn = createContext(language = "en")
        val ctxDe = createContext(language = "de")

        val htmlEn = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctxEn)
        val htmlDe = WidgetBuilder.build(listOf(WidgetType.SERVER_STATS), ctxDe)

        // Both should render successfully
        assertNotNull(htmlEn)
        assertNotNull(htmlDe)

        // They should be different (different language)
        assertNotEquals(htmlEn, htmlDe)
    }

    // endregion
}