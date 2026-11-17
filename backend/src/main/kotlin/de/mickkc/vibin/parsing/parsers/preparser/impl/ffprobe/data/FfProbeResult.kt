package de.mickkc.vibin.parsing.parsers.preparser.impl.ffprobe.data

import kotlinx.serialization.Serializable

@Serializable
data class FfProbeResult(
    val streams: List<FfProbeStream>,
    val format: FfprobeFormat
)
