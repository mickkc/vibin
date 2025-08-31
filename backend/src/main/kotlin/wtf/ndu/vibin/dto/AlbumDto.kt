package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto (
    val id: Long,
    val title: String,
    val artists: List<ArtistDto>,
    val cover: ImageDto?,
    val songsAmount: Long,
    val createdAt: Long,
    val updatedAt: Long?
)