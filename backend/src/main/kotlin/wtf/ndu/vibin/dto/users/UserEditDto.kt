package wtf.ndu.vibin.dto.users

import kotlinx.serialization.Serializable

@Serializable
data class UserEditDto(
    val username: String?,
    val displayName: String?,
    val email: String?,
    val isAdmin: Boolean?,
    val isActive: Boolean?,
    val profilePictureUrl: String?,
    val password: String?
)