package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.parsing.TrackInfoMetadata

interface FileParser {

    suspend fun parse(data: PreparseData): TrackInfoMetadata?

}