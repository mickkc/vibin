package de.mickkc.vibin.dto

import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackDto

data class ControlSessionDto (
    val sessionId: String,
    val userId: Long,
    val playing: Boolean,
    val currentTrack: TrackDto?,
    val queue: List<MinimalTrackDto>,
    val lastUpdate: Long
)