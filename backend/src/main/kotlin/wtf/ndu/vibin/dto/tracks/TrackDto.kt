package wtf.ndu.vibin.dto.tracks

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.ArtistDto
import wtf.ndu.vibin.dto.ImageDto
import wtf.ndu.vibin.dto.TagDto
import wtf.ndu.vibin.dto.users.UserDto
import wtf.ndu.vibin.dto.albums.AlbumDto

@Serializable
data class TrackDto (
    val id: Long,
    val title: String,
    val album: AlbumDto,
    val artists: List<ArtistDto>,
    val explicit: Boolean,
    val trackNumber: Int?,
    val trackCount: Int?,
    val discNumber: Int?,
    val discCount: Int?,
    val year: Int?,
    val duration: Long?,
    val comment: String?,
    val cover: ImageDto?,
    val path: String,
    val checksum: String,
    val tags: List<TagDto>,
    val uploader: UserDto?,
    val createdAt: Long,
    val updatedAt: Long?
)