package wtf.ndu.vibin.dto.users

import kotlinx.serialization.Serializable
import wtf.ndu.vibin.dto.ImageDto

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val displayName: String,
    val email: String?,
    val isActive: Boolean,
    val isAdmin: Boolean,
    val lastLogin: Long?,
    val profilePicture: ImageDto?,
    val createdAt: Long,
    val updatedAt: Long?
)