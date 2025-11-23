package de.mickkc.vibin.parsing.parsers.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerTrackData(
    val id: Long,
    val title: String,
    val duration: Double,
    @Suppress("PropertyName") val explicit_lyrics: Boolean,
    val artist: DeezerArtistData,
    val album: DeezerAlbumData
)

@Serializable
data class DeezerArtistData(
    val id: Long,
    val name: String
)

@Suppress("PropertyName")
@Serializable
data class DeezerAlbumData(
    val id: Long,
    val title: String,
    val cover: String,
    val cover_small: String,
    val cover_medium: String,
    val cover_big: String,
    val cover_xl: String
)

@Serializable
data class DeezerTrackInfo(
    val id: Long,
    val title: String,
    val track_position: Int,
    val disk_number: Int,
    val release_date: String,
    val explicit_lyrics: Boolean,
    val contributors: List<DeezerArtistData>,
    val album: DeezerAlbumData
)