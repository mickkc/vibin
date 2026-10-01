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

class MostListenedWidgetTest {

    @Test
    fun testMostListenedToTracksWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val track1 = TrackTestUtils.createTrack("Some track", "Some Album", "Some artist", duration = 100)
        val track2 = TrackTestUtils.createTrack("Another track", "Another album", "Another artist", duration = 100)

        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, DateTimeUtils.now() - 300)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now())

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_TRACKS), ctx)

        testItems(
            html,
            TestItemCheck("Another track", "Another artist", "🥇"),
            TestItemCheck("Some track", "Some artist", "🥈")
        )
    }

    @Test
    fun testMostListenedToTracksWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_TRACKS), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }

    @Test
    fun testMostListenedToArtistsWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val track1 = TrackTestUtils.createTrack("Some track", "Some Album", "Some artist, Another artist", duration = 100)
        val track2 = TrackTestUtils.createTrack("Another track", "Another album", "Another artist", duration = 100)

        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now())

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_ARTISTS), ctx)

        testItems(
            html,
            TestItemCheck("Another artist", "Artist", "🥇"),
            TestItemCheck("Some artist", "Artist", "🥈")
        )
    }

    @Test
    fun testMostListenedToArtistsWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_ARTISTS), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }

    @Test
    fun testMostListenedToAlbumsWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val album1 = AlbumTestUtils.createAlbum("Some album")
        val album2 = AlbumTestUtils.createAlbum("Another album")

        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, album1.id.value, ListenType.ALBUM, DateTimeUtils.now() - 300)
        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now())

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_ALBUM), ctx)

        testItems(
            html,
            TestItemCheck("Another album", "Album", "🥇"),
            TestItemCheck("Some album", "Album", "🥈")
        )
    }

    @Test
    fun testMostListenedToAlbumsWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_ALBUM), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }

    @Test
    fun testMostListenedToWidget() = testApp { _ ->
        val user = UserRepo.getById(1)!!

        val album1 = AlbumTestUtils.createAlbum("Some album")
        val album2 = AlbumTestUtils.createAlbum("Another album")

        val artist1 = ArtistTestUtils.createArtist("Some artist")

        val track1 = TrackTestUtils.createTrack("Some track", "Some album", "Some artist, Another artist", duration = 100)
        val track2 = TrackTestUtils.createTrack("Another track", "Another album", "Another artist")

        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now() - 500)
        ListenRepo.listenedTo(user.id.value, album1.id.value, ListenType.ALBUM, DateTimeUtils.now() - 300)
        ListenRepo.listenedTo(user.id.value, album2.id.value, ListenType.ALBUM, DateTimeUtils.now())

        ListenRepo.listenedTo(user.id.value, track1.id.value, ListenType.TRACK, DateTimeUtils.now() - 1000)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now() - 400)
        ListenRepo.listenedTo(user.id.value, track2.id.value, ListenType.TRACK, DateTimeUtils.now() - 100)

        ListenRepo.listenedTo(user.id.value, artist1.id.value, ListenType.ARTIST, DateTimeUtils.now() - 100)
        ListenRepo.listenedTo(user.id.value, artist1.id.value, ListenType.ARTIST, DateTimeUtils.now() - 1200)

        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_TO), ctx)

        testItems(
            html,
            TestItemCheck("Another track", "Another artist", "🥇"),
            TestItemCheck("Another album", "Album", "🥈"),
            TestItemCheck("Some artist", "Artist", "🥉"),
            TestItemCheck("Some track", "Some artist, Another artist"),
            TestItemCheck("Some album", "Album")
        )
    }

    @Test
    fun testMostListenedToWidget_Empty() = testApp { _ ->
        val ctx = WidgetTestUtils.createContext()
        val html = WidgetBuilder.build(listOf(WidgetType.MOST_LISTENED_TO), ctx)

        // Validates that the list is there but empty
        testItems(html)
    }
}