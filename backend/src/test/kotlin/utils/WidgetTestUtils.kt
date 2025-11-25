package utils

import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.repos.FavoriteRepo

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
}