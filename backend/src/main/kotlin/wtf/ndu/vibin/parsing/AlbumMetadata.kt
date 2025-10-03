package wtf.ndu.vibin.parsing

import kotlinx.serialization.Serializable

@Serializable
data class AlbumMetadata(
    val title: String,
    val coverImageUrl: String?,
    val artistName: String?
)
