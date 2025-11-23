package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsDto(
    val totalTracks: Long,
    val totalTrackDuration: Long,
    val totalArtists: Long,
    val totalAlbums: Long,
    val totalPlaylists: Long,
    val totalUsers: Long,
    val totalPlays: Long,
)
