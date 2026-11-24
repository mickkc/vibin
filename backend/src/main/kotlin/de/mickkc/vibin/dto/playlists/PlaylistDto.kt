package de.mickkc.vibin.dto.playlists

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.users.UserDto
import io.ktor.openapi.JsonSchema

@Serializable
data class PlaylistDto(

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"Example Playlist\"")
    val name: String,

    @JsonSchema.Example("\"Some description\"")
    val description: String,

    @JsonSchema.Example("true")
    val public: Boolean,

    val collaborators: List<UserDto>,

    @JsonSchema.Example("\"+pop\"")
    val vibedef: String?,

    val owner: UserDto,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val updatedAt: Long?
)