package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class Streamable(
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long?,
    val volumeOffset: Double?,
    val coverChecksum: String?,
    val coverSignature: String?,
    val coverExpiration: Long?,
    val streamSignature: String,
    val streamExpiration: Long
)
