package wtf.ndu.vibin.parsing

import kotlinx.serialization.Serializable

@Serializable
data class TrackInfoMetadata (
    val title: String,
    val artistNames: List<String>?,
    val albumName: String?,
    val trackNumber: Int? = null,
    val trackCount: Int? = null,
    val discNumber: Int? = null,
    val discCount: Int? = null,
    val year: Int? = null,
    val tags: List<String>? = null,
    val comment: String? = null,
    val coverImageUrl: String? = null,
    val explicit: Boolean? = null
)