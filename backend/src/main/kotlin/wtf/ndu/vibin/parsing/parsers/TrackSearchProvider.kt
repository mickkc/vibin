package wtf.ndu.vibin.parsing.parsers

import wtf.ndu.vibin.parsing.TrackInfoMetadata

interface TrackSearchProvider {
    suspend fun searchTrack(query: String): List<TrackInfoMetadata>?
}