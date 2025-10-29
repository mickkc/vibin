package wtf.ndu.vibin.dto.tracks

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.IdOrNameDto

@Serializable
data class TrackEditDto(
    val title: String?,
    val explicit: Boolean?,
    val trackNumber: Int?,
    val trackCount: Int?,
    val discNumber: Int?,
    val discCount: Int?,
    val year: Int?,
    val comment: String?,
    val imageUrl: String?,
    val album: IdOrNameDto?,
    val artists: List<IdOrNameDto>?,
    val tags: List<IdOrNameDto>?,
    val lyrics: String?
)