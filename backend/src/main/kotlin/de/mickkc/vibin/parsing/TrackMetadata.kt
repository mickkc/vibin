package de.mickkc.vibin.parsing

import de.mickkc.vibin.parsing.parsers.PreparseData

data class TrackMetadata (
    val fileInfo: PreparseData?,
    val trackInfo: TrackInfoMetadata
)