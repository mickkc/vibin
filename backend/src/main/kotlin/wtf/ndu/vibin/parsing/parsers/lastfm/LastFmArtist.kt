package wtf.ndu.vibin.parsing.parsers.lastfm

import kotlinx.serialization.Serializable

@Serializable
data class LastFmArtistResults(
    val artistmatches: ArtistMatches
)

@Serializable
data class ArtistMatches(
    val artist: List<LastFmArtist>
)

@Serializable
data class LastFmArtist(
    val name: String,
    val image: List<LastFmImage>
)

@Serializable
data class LastFmArtistInfo(
    val artist: LastFmArtistDetail
)

@Serializable
data class LastFmArtistDetail(
    val name: String,
    val image: List<LastFmImage>,
    val bio: LastFmWiki?,
)