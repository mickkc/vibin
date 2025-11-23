package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.users.UserDto

@Serializable
data class LoginResultDto (
    val success: Boolean,
    val token: String,
    val user: UserDto,
    val permissions: List<String>
)