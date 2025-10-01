package wtf.ndu.vibin.dto.albums

import kotlinx.serialization.Serializable

@Serializable
data class AlbumEditDto(
    val title: String?,
    val coverUrl: String?
)
