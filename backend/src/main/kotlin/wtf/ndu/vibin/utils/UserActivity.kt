package wtf.ndu.vibin.utils

import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.tracks.TrackEntity

data class UserActivity(
    val recentTracks: List<TrackEntity>,
    val topTracks: List<TrackEntity>,
    val topArtists: List<ArtistEntity>
)
