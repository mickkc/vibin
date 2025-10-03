package wtf.ndu.vibin.parsing.parsers

import wtf.ndu.vibin.parsing.TrackInfoMetadata

interface FileParser {

    suspend fun parse(data: PreparseData): TrackInfoMetadata?

}