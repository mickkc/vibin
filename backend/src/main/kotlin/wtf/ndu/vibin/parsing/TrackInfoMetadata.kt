package wtf.ndu.vibin.parsing

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.IdOrNameDto

@Serializable
data class TrackInfoMetadata (
    var title: String,
    var artists: List<IdOrNameDto>?,
    var album: IdOrNameDto?,
    var trackNumber: Int? = null,
    var trackCount: Int? = null,
    var discNumber: Int? = null,
    var discCount: Int? = null,
    var year: Int? = null,
    var tags: List<IdOrNameDto>? = null,
    var comment: String? = null,
    var coverImageUrl: String? = null,
    var explicit: Boolean? = null,
    var lyrics: String? = null
)