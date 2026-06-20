package de.mickkc.vibin.dto

import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackDto

data class ControlSessionDto (
    val sessionId: String,
    val userId: Long,
    val state: String,
    val currentTrack: TrackDto?,
    val queue: List<MinimalTrackDto>,
    val shuffle: Boolean,
    val repeat: String,
    val speed: Float,
    val volume: Float,
    val lastUpdate: Long
)