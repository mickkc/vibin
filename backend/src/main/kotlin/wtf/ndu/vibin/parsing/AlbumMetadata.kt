package wtf.ndu.vibin.parsing

import kotlinx.serialization.Serializable

@Serializable
data class AlbumMetadata(
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val artistName: String?,
    val year: Int? = null,
    val isSingle: Boolean? = null,
)
