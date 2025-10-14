package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class UserActivityDto(
    val recentTracks: List<MinimalTrackDto>,
    val topTracks: List<MinimalTrackDto>,
    val topArtists: List<ArtistDto>
)
