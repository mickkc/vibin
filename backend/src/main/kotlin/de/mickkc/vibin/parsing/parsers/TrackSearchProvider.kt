package de.mickkc.vibin.parsing.parsers

import de.mickkc.vibin.parsing.TrackInfoMetadata

interface TrackSearchProvider {
    suspend fun searchTrack(query: String): List<TrackInfoMetadata>?
}