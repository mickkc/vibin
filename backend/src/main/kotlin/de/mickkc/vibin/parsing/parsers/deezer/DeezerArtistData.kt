package de.mickkc.vibin.parsing.parsers.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerArtistMetadata (
    val name: String,
    @Suppress("PropertyName") val picture_xl: String?
)