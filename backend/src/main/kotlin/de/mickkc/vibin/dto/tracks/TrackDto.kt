package de.mickkc.vibin.dto.tracks

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.ArtistDto
import de.mickkc.vibin.dto.tags.TagDto
import de.mickkc.vibin.dto.users.UserDto
import de.mickkc.vibin.dto.albums.AlbumDto

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
    val bitrate: Int?,
    val path: String,
    val sampleRate: Int?,
    val channels: Int?,
    val tags: List<TagDto>,
    val uploader: UserDto?,
    val hasLyrics: Boolean,
    val createdAt: Long,
    val updatedAt: Long?
)