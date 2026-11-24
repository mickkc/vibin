package de.mickkc.vibin.dto.tracks

import de.mickkc.vibin.db.tracks.TrackVisibility
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.artists.ArtistDto
import de.mickkc.vibin.dto.tags.TagDto
import de.mickkc.vibin.dto.users.UserDto
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class TrackDto (

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"Never Gonna Give You Up\"")
    val title: String,

    val album: AlbumDto,

    val artists: List<ArtistDto>,

    @JsonSchema.Example("1")
    val coverId: Long?,

    @JsonSchema.Example("false")
    val explicit: Boolean,

    @JsonSchema.Example("1")
    val trackNumber: Int?,

    @JsonSchema.Example("5")
    val trackCount: Int?,

    @JsonSchema.Example("1")
    val discNumber: Int?,

    @JsonSchema.Example("1")
    val discCount: Int?,

    @JsonSchema.Example("1987")
    val year: Int?,

    @JsonSchema.Example("215000")
    @JsonSchema.Description("Track duration in milliseconds")
    val duration: Long?,

    @JsonSchema.Example("\"Rickroll :D\"")
    val comment: String?,

    @JsonSchema.Example("320")
    @JsonSchema.Description("Bitrate in kbps")
    val bitrate: Int?,

    @JsonSchema.Example("\"Rick Astley/Never Gonna Give You Up.mp3\"")
    val path: String,

    @JsonSchema.Example("44000")
    val sampleRate: Int?,

    @JsonSchema.Example("2")
    @JsonSchema.Description("1 = mono, 2 = stereo")
    val channels: Int?,

    val tags: List<TagDto>,

    val uploader: UserDto?,

    @JsonSchema.Example("true")
    val hasLyrics: Boolean,

    @JsonSchema.Example("\"Public\"")
    val visibility: TrackVisibility,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val updatedAt: Long?
)