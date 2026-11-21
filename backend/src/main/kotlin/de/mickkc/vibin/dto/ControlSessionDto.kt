package de.mickkc.vibin.dto

import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackDto
import io.ktor.openapi.JsonSchema

data class ControlSessionQueueItemInDto (
    val id: String,
    @JsonSchema.Example("0")
    val pos: Int,
    @JsonSchema.Example("1")
    val trackId: Long?
)

data class ControlSessionQueueItemOutDto (
    val id: String,
    val track: MinimalTrackDto?
)

data class ControlSessionDto (

    @JsonSchema.Example("\"970e6075a413f042\"")
    val sessionId: String,

    @JsonSchema.Example("1")
    val userId: Long,

    @JsonSchema.Enum("PLAYING", "PAUSED", "STOPPED", "BUFFERING", "UNKNOWN")
    @JsonSchema.Example("\"PLAYING\"")
    val state: String,

    val currentTrack: TrackDto?,

    val queue: List<ControlSessionQueueItemOutDto>,

    @JsonSchema.Example("true")
    val shuffle: Boolean,

    @JsonSchema.Enum("OFF", "CURRENT_TRACK", "ALL")
    @JsonSchema.Example("\"ALL\"")
    val repeat: String,

    @JsonSchema.Example("1")
    @JsonSchema.Minimum(0.1)
    @JsonSchema.Maximum(2.0)
    val speed: Float,

    @JsonSchema.Example("1")
    @JsonSchema.Minimum(0.0)
    @JsonSchema.Maximum(1.0)
    val volume: Float,

    @JsonSchema.Example("1782511260")
    val lastUpdate: Long
)