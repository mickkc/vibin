package de.mickkc.vibin.parsing.parsers.preparser.impl.ffprobe.data

import kotlinx.serialization.Serializable

@Serializable
data class FfprobeFormat(
    val filename: String,
    val nb_streams: String?,
    val format_name: String?,
    val duration: String?,
    val size: String,
    val bit_rate: String?
)
