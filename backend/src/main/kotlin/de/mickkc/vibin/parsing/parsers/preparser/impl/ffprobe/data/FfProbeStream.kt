package de.mickkc.vibin.parsing.parsers.preparser.impl.ffprobe.data

import kotlinx.serialization.Serializable

@Serializable
data class FfProbeStream(
    val index: Int,
    val codec_name: String,
    val codec_type: String,
    val sample_rate: String?,
    val channels: String?,
    val duration: String?,
    val bit_rate: String?,
    val tags: FfProbeStreamTags?
)
