package wtf.ndu.vibin.dto.albums

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.ArtistDto
import wtf.ndu.vibin.dto.ImageDto

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