package wtf.ndu.vibin.dto.tracks

import kotlinx.serialization.Serializable

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
    val albumId: Long?,
    val artistIds: List<Long>?,
)