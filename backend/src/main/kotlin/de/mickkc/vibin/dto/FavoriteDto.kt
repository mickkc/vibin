package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class FavoriteDto (
    val tracks: List<MinimalTrackDto?>,
    val albums: List<AlbumDto?>,
    val artists: List<ArtistDto?>,
)