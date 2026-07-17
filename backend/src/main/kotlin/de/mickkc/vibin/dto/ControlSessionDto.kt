package de.mickkc.vibin.dto

import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackDto

data class ControlSessionQueueItemInDto (
    val id: String,
    val pos: Int,
    val trackId: Long?
)

data class ControlSessionQueueItemOutDto (
    val id: String,
    val track: MinimalTrackDto?
)

data class ControlSessionDto (
    val sessionId: String,
    val userId: Long,
    val state: String,
    val currentTrack: TrackDto?,
    val queue: List<ControlSessionQueueItemOutDto>,
    val shuffle: Boolean,
    val repeat: String,
    val speed: Float,
    val volume: Float,
    val lastUpdate: Long
)