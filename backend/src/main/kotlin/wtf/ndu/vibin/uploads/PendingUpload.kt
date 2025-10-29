package wtf.ndu.vibin.uploads

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.IdOrNameDto

@Serializable
data class PendingUpload(
    val id: String,
    val filePath: String,
    var title: String,
    var album: IdOrNameDto,
    var artists: List<IdOrNameDto>,
    var tags: List<IdOrNameDto>,
    var explicit: Boolean,
    var trackNumber: Int?,
    var trackCount: Int?,
    var discNumber: Int?,
    var discCount: Int?,
    var year: Int?,
    var comment: String,
    var lyrics: String?,
    var coverUrl: String?,
    var uploaderId: Long,
    var lastUpdated: Long
)
