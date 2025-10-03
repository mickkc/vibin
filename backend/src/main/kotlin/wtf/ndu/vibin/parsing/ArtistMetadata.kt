package wtf.ndu.vibin.parsing

import kotlinx.serialization.Serializable

@Serializable
data class ArtistMetadata (
    val name: String,
    val pictureUrl: String?,
    val biography: String? = null
)