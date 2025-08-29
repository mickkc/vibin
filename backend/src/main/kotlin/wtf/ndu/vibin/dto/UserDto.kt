package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

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