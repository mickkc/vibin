package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginResultDto (
    val success: Boolean,
    val token: String,
    val user: UserDto,
    val permissions: List<String>
)