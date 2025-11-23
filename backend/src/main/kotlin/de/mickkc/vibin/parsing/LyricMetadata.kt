package de.mickkc.vibin.parsing

import kotlinx.serialization.Serializable

@Serializable
data class LyricMetadata(
    val title: String?,
    val artistName: String?,
    val albumName: String?,
    val content: String,
    val synced: Boolean,
    val duration: Long?
)
