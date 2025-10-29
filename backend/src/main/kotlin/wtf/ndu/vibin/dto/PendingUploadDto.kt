package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.tags.TagDto
import wtf.ndu.vibin.dto.users.UserDto

@Serializable
data class PendingUploadDto(
    val id: Long,
    val filePath: String,
    val title: String,
    val album: String,
    val artists: List<String>,
    val explicit: Boolean,
    val trackNumber: Int?,
    val trackCount: Int?,
    val discNumber: Int?,
    val discCount: Int?,
    val year: Int?,
    val comment: String,
    val coverUrl: String?,
    val uploader: UserDto,
    val tags: List<TagDto>
)
