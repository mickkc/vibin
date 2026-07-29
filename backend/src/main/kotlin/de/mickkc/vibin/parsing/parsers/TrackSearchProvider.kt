package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.parsing.TrackInfoMetadata

interface TrackSearchProvider : BaseMetadataProvider {
    suspend fun searchTrack(query: String): List<TrackInfoMetadata>?
}