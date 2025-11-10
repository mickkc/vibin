package wtf.ndu.vibin.dto.tracks

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.IdNameDto

@Serializable
data class MinimalTrackDto (
    val id: Long,
    val title: String,
    val artists: List<IdNameDto>,
    val album: IdNameDto,
    val duration: Long?,
    val uploader: IdNameDto?
)