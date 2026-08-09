package de.mickkc.vibin.dto.users

import kotlinx.serialization.Serializable

@Serializable
data class UserEditDto(
    val username: String? = null,
    val displayName: String? = null,
    val description: String? = null,
    val email: String? = null,
    val isAdmin: Boolean? = null,
    val isActive: Boolean? = null,
    val profilePictureUrl: String? = null,
    val oldPassword: String? = null,
    val password: String? = null
)