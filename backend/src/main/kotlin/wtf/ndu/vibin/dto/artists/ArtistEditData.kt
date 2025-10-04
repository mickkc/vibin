package wtf.ndu.vibin.dto.artists

import kotlinx.serialization.Serializable

@Serializable
data class ArtistEditData (
    val name: String?,
    val description: String?,
    val imageUrl: String?
)