package wtf.ndu.vibin.dto.albums

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.ArtistDto
import wtf.ndu.vibin.dto.ImageDto

@Serializable
data class AlbumDto (
    val id: Long,
    val title: String,
    val description: String,
    val year: Int?,
    val artists: List<ArtistDto>,
    val cover: ImageDto?,
    val trackCount: Long,
    val createdAt: Long,
    val updatedAt: Long?
)