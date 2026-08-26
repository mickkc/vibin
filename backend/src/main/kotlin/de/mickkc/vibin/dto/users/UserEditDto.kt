package de.mickkc.vibin.dto.users

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
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
) : ValidatableBaseDto {
    override fun validate() {
        if (username != null && username.length > 255)
            invalidParameter("username", "username.length <= 255")

        if (displayName != null && displayName.length > 255)
            invalidParameter("displayName", "displayName.length <= 255")

        if (email != null && email.length > 255)
            invalidParameter("email", "email.length <= 255")
    }
}