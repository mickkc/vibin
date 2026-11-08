package de.mickkc.vibin.scrobbling.listenbrainz.data

import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzRequest(
    val listen_type: String,
    val payload: List<ListenBrainzPayload>
)
