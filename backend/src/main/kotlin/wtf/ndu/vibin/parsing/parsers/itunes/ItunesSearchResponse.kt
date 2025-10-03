package wtf.ndu.vibin.parsing.parsers.itunes

import kotlinx.serialization.Serializable

@Serializable
data class ItunesSearchResponse<T> (
    val resultCount: Int,
    val results: List<T>
)