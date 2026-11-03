package de.mickkc.vibin.dto.scrobbling

import de.mickkc.vibin.scrobbling.ScrobblerStatus
import kotlinx.serialization.Serializable

@Serializable
data class ScrobblerDto (
    val id: String,
    val name: String,
    val available: Boolean,
    val status: ScrobblerStatus?
)