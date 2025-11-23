package de.mickkc.vibin.dto.users

import kotlinx.serialization.Serializable

@Serializable
data class UserEditDto(
    val username: String?,
    val displayName: String?,
    val description: String?,
    val email: String?,
    val isAdmin: Boolean?,
    val isActive: Boolean?,
    val profilePictureUrl: String?,
    val oldPassword: String?,
    val password: String?
)