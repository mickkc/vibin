package de.mickkc.vibin.scrobbling

import kotlinx.serialization.Serializable

@Serializable
data class ScrobblerStatus(
    val error: Boolean,
    val message: String?
) {
    companion object {
        fun error(message: String) = ScrobblerStatus(true, message)
        fun success() = ScrobblerStatus(false, null)
    }
}
