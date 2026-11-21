package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class StatisticsDto(

    @JsonSchema.Example("100")
    val totalTracks: Long,

    @JsonSchema.Example("12240000")
    @JsonSchema.Description("Combined duration of all tracks in milliseconds")
    val totalTrackDuration: Long,

    @JsonSchema.Example("20")
    val totalArtists: Long,

    @JsonSchema.Example("15")
    val totalAlbums: Long,

    @JsonSchema.Example("5")
    val totalPlaylists: Long,

    @JsonSchema.Example("2")
    val totalUsers: Long,

    @JsonSchema.Example("1354")
    val totalPlays: Long,

    @JsonSchema.Example("138108")
    @JsonSchema.Description("The combined total listening time of all users in seconds")
    val totalListenDuration: Long
)
