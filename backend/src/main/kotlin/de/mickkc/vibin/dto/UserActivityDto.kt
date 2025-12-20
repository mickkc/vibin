package de.mickkc.vibin.dto

import de.mickkc.vibin.dto.artists.ArtistDto
import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class UserActivityDto(
    val recentTracks: List<MinimalTrackDto>,
    val topTracks: List<MinimalTrackDto>,
    val topArtists: List<ArtistDto>
)
