package wtf.ndu.vibin.parsing.parsers.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchResponse(
    val data: List<DeezerTrackData>,
    val total: Int,
    val next: String?
)
