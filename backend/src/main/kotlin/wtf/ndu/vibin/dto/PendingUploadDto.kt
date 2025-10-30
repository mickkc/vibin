package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.albums.AlbumDto
import wtf.ndu.vibin.dto.tags.TagDto

@Serializable
data class PendingUploadDto(
    val id: String,
    val filePath: String,
    var title: String,
    var album: AlbumDto?,
    var artists: List<ArtistDto>,
    var tags: List<TagDto>,
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
