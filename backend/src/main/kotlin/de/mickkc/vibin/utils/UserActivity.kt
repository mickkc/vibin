package de.mickkc.vibin.utils

import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.tracks.TrackEntity

data class UserActivity(
    val recentTracks: List<TrackEntity>,
    val topTracks: List<TrackEntity>,
    val topArtists: List<ArtistEntity>
)
