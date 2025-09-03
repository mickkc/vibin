package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class MinimalTrackDto (
    val id: Long,
    val title: String,
    val artists: List<IdNameDto>,
    val album: IdNameDto,
    val duration: Long?,
    val cover: ImageDto?,
    val uploader: IdNameDto?
)