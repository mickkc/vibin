package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDto(
    val id: Long,
    val name: String,
    val description: String,
    val public: Boolean,
    val collaborators: List<UserDto>,
    val cover: ImageDto?,
    val hasVibeDef: Boolean,
    val owner: UserDto,
    val createdAt: Long,
    val updatedAt: Long?
)
