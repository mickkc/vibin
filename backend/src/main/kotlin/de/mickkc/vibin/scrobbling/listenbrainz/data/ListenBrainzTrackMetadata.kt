package de.mickkc.vibin.scrobbling.listenbrainz.data

import kotlinx.serialization.Serializable

@Serializable
data class ListenBrainzTrackMetadata(
    val artist_name: String,
    val track_name: String,
    val release_name: String,
    val additional_info: ListenBrainzAdditionalInfo
)
