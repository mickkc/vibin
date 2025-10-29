package wtf.ndu.vibin.parsing

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.IdOrNameDto

@Serializable
data class TrackInfoMetadata (
    val title: String,
    val artists: List<IdOrNameDto>?,
    val album: IdOrNameDto?,
    val trackNumber: Int? = null,
    val trackCount: Int? = null,
    val discNumber: Int? = null,
    val discCount: Int? = null,
    val year: Int? = null,
    val tags: List<IdOrNameDto>? = null,
    val comment: String? = null,
    val coverImageUrl: String? = null,
    val explicit: Boolean? = null,
    val lyrics: String? = null
)