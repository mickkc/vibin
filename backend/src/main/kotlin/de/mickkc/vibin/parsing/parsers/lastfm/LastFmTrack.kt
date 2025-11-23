package de.mickkc.vibin.parsing.parsers.lastfm

import kotlinx.serialization.Serializable

@Serializable
data class LastFmTrackResults(
    val trackmatches: TrackMatches
)

@Serializable
data class TrackMatches(
    val track: List<LastFmTrack>
)

@Serializable
data class LastFmTrackInfo(
    val track: LastFmTrackDetail
)

@Serializable
data class LastFmTrackDetail(
    val name: String,
    val artist: LastFmTrackArtist,
    val album: LastFmTrackAlbum,
    val wiki: LastFmWiki?,
    val toptags: LastFmTrackTopTags,
)

@Serializable
data class LastFmTrackTopTags(
    val tag: List<LastFmTrackTag>,
)

@Serializable
data class LastFmTrackTag(
    val name: String,
)

@Serializable
data class LastFmTrackArtist(
    val name: String,
)

@Serializable
data class LastFmTrackAlbum(
    val title: String,
    val image: List<LastFmImage>,
)

@Serializable
data class LastFmTrack(
    val name: String,
    val artist: String,
    val mbid: String,
    val image: List<LastFmImage>,
)
