package de.mickkc.vibin.dto.users

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val displayName: String?,
    val description: String,
    val imageId: Long?,
    val email: String?,
    val emailVerified: Boolean,
    val isActive: Boolean,
    val isAdmin: Boolean,
    val lastLogin: Long?,
    val createdAt: Long,
    val updatedAt: Long?
)