package wtf.ndu.vibin.parsing.parsers.lastfm

import kotlinx.serialization.Serializable

@Serializable
data class LastFmResults<T>(
    val results: T
)

@Serializable
data class LastFmWiki(
    val summary: String,
    val content: String
)

@Serializable
data class LastFmImage(
    val `#text`: String,
    val size: String
)

