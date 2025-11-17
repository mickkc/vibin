package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.albums.AlbumDto
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto

@Serializable
data class FavoriteDto (
    val tracks: List<MinimalTrackDto?>,
    val albums: List<AlbumDto?>,
    val artists: List<ArtistDto?>,
)