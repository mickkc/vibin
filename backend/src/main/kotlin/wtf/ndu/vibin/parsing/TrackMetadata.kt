package wtf.ndu.vibin.parsing

import wtf.ndu.vibin.parsing.parsers.PreparseData

data class TrackMetadata (
    val fileInfo: PreparseData?,
    val trackInfo: TrackInfoMetadata
)