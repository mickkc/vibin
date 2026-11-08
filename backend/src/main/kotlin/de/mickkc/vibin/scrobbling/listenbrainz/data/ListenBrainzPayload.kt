package de.mickkc.vibin.scrobbling.listenbrainz.data

import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzPayload(
    val listened_at: Long?,
    val track_metadata: ListenBrainzTrackMetadata
)
