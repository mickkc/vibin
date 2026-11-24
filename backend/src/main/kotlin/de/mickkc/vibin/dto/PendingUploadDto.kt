package de.mickkc.vibin.dto

import de.mickkc.vibin.db.tracks.TrackVisibility
import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.artists.ArtistDto
import de.mickkc.vibin.dto.tags.TagDto
import io.ktor.openapi.JsonSchema

@Serializable
data class PendingUploadDto(

    @JsonSchema.Example("\"45c258ee-396f-443f-b127-056f54c28e19\"")
    val id: String,

    @JsonSchema.Example("\"1/Example.mp3\"")
    val filePath: String,

    @JsonSchema.Example("\"Never Gonna Give You Up\"")
    var title: String,

    var album: AlbumDto?,

    var artists: List<ArtistDto>,

    var tags: List<TagDto>,

    @JsonSchema.Example("false")
    var explicit: Boolean,

    @JsonSchema.Example("1")
    var trackNumber: Int?,

    @JsonSchema.Example("5")
    var trackCount: Int?,

    @JsonSchema.Example("1")
    var discNumber: Int?,

    @JsonSchema.Example("1")
    var discCount: Int?,

    @JsonSchema.Example("1987")
    var year: Int?,

    @JsonSchema.Example("\"Rickroll :D\"")
    var comment: String,

    @JsonSchema.Example("\"[00:19.73]We're no strangers to love\n" +
            "[00:23.66]You know the rules and so do I" +
            "...\"")
    var lyrics: String?,

    @JsonSchema.Example("\"Public\"")
    var visibility: TrackVisibility,

    @JsonSchema.Example("1")
    var uploaderId: Long,

    @JsonSchema.Example("1782511260")
    var lastUpdated: Long
)
