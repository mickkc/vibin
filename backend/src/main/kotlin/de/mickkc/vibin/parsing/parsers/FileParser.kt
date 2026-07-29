package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.parsing.TrackInfoMetadata

interface FileParser : BaseMetadataProvider {

    suspend fun parse(data: PreparseData): TrackInfoMetadata?

}