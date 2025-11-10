package wtf.ndu.vibin.dto.playlists

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.users.UserDto

@Serializable
data class PlaylistDto(
    val id: Long,
    val name: String,
    val description: String,
    val public: Boolean,
    val collaborators: List<UserDto>,
    val vibedef: String?,
    val owner: UserDto,
    val createdAt: Long,
    val updatedAt: Long?
)