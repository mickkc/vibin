package widgets

import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.repos.ListenRepo
import de.mickkc.vibin.repos.UserRepo
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.widgets.WidgetBuilder
import de.mickkc.vibin.widgets.WidgetType
import org.junit.Test
import utils.*
import utils.WidgetTestUtils.testItems
import utils.WidgetTestUtils.TestItemCheck

class RecentlyListenedWidgetTest {

    @Test
    fun testRecentlyListenedToTracksWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val track1 = TrackTestUtils.createTrack("Some track", "Some Album", "Some artist", duration = 100)
        val track2 = TrackTestUtils.createTrack("Another track", "Another album", "Another artist", duration = 100)

        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, DateTimeUtils.now() - 300)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now())

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENT_TRACKS), ctx)

        testItems(
            html,
            TestItemCheck("Another track", "Another artist"),
            TestItemCheck("Some track", "Some artist")
        )
    }

    @Test
    fun testRecentlyListenedToTracksWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENT_TRACKS), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }

    @Test
    fun testRecentlyListenedToArtistsWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val track1 = TrackTestUtils.createTrack("Some track", "Some Album", "Some artist, Another artist", duration = 100)
        val track2 = TrackTestUtils.createTrack("Another track", "Another album", "Another artist", duration = 100)

        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now())

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENT_ARTISTS), ctx)

        testItems(
            html,
            TestItemCheck("Another artist", "Artist"),
            TestItemCheck("Some artist", "Artist")
        )
    }

    @Test
    fun testRecentlyListenedToArtistsWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENT_ARTISTS), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }

    @Test
    fun testRecentlyListenedToAlbumsWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val album1 = AlbumTestUtils.createAlbum("Some album")
        val album2 = AlbumTestUtils.createAlbum("Another album")

        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, album1.id.value, ListenType.ALBUM, DateTimeUtils.now() - 300)
        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now())

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENT_ALBUMS), ctx)

        testItems(
            html,
            TestItemCheck("Another album", "Album"),
            TestItemCheck("Some album", "Album")
        )
    }

    @Test
    fun testRecentlyListenedToAlbumsWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENT_ALBUMS), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }

    @Test
    fun testRecentlyListenedToWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val album1 = AlbumTestUtils.createAlbum("Some album")
        val album2 = AlbumTestUtils.createAlbum("Another album")

        val artist1 = ArtistTestUtils.createArtist("Some artist")

        val track1 = TrackTestUtils.createTrack("Some track", "Some album", "Some artist, Another artist", duration = 100)
        val track2 = TrackTestUtils.createTrack("Another track", "Another album", "Another artist")

        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now() - 1500)
        ListenRepo.listenedTo(user.id.value, album1.id.value, ListenType.ALBUM, DateTimeUtils.now() - 1300)

        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, DateTimeUtils.now() - 1100)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now() - 900)

        ListenRepo.listenedTo(user.id.value, artist1.id.value, ListenType.ARTIST, DateTimeUtils.now() - 700)
        ListenRepo.listenedTo(user.id.value, artist1.id.value, ListenType.ARTIST, DateTimeUtils.now() - 500)

        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now() - 300)
        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now() - 100)

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENTLY_LISTENED), ctx)

        testItems(
            html,
            TestItemCheck("Another album", "Album"),
            TestItemCheck("Another track", "Another artist"),
            TestItemCheck("Some artist", "Artist"),
            TestItemCheck("Some track", "Some artist, Another artist"),
            TestItemCheck("Some album", "Album"),
        )
    }

    @Test
    fun testRecentlyListenedToWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.RECENTLY_LISTENED), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }
}