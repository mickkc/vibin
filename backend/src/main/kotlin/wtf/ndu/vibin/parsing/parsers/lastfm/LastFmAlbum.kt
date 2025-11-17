package wtf.ndu.vibin.parsing.parsers.lastfm

import kotlinx.serialization.Serializable

@Serializable
data class LastFmAlbumResults(
    val albummatches: AlbumMatches
)

@Serializable
data class AlbumMatches(
    val album: List<LastFmAlbum>
)

@Serializable
data class LastFmAlbum(
    val name: String,
    val artist: String,
    val mbid: String,
    val image: List<LastFmImage>
)

@Serializable
data class LastFmAlbumInfo(
    val album: LastFmAlbumDetail
)

@Serializable
data class LastFmAlbumDetail(
    val name: String,
    val artist: String,
    val mbid: String,
    val image: List<LastFmImage>,
    val wiki: LastFmWiki?
)