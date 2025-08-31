package wtf.ndu.vibin.parsing.parsers.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerArtistMetadata (
    val name: String,
    @Suppress("PropertyName") val picture_big: String?
)