package de.mickkc.vibin.dto.users

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class UserEditDto(

    @JsonSchema.Example("\"mickkc\"")
    @JsonSchema.MaxLength(255)
    val username: String? = null,

    @JsonSchema.Example("\"Mickkc\"")
    @JsonSchema.MaxLength(255)
    val displayName: String? = null,

    @JsonSchema.Example("\"I am Mickkc.\"")
    val description: String? = null,

    @JsonSchema.Example("\"mickkc@example.com\"")
    val email: String? = null,

    @JsonSchema.Example("true")
    @JsonSchema.Description("Can only be set to true by other admins")
    @JsonSchema.MaxLength(255)
    val isAdmin: Boolean? = null,

    @JsonSchema.Example("true")
    val isActive: Boolean? = null,

    @JsonSchema.Example("\"https://example.org/images/example.png\"")
    val profilePictureUrl: String? = null,

    @JsonSchema.Example("\"hunter2\"")
    val oldPassword: String? = null,

    @JsonSchema.Example("\"hunter3\"")
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