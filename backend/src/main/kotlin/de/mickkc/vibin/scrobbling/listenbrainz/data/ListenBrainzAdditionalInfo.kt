package de.mickkc.vibin.scrobbling.listenbrainz.data

import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzAdditionalInfo(
    val duration_ms: Long?,
    val duration_played: Long?,
    val music_service_name: String
)
