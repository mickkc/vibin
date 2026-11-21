package de.mickkc.vibin.dto.users

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(

    @JsonSchema.Example("1")
    val id: Long,

    @JsonSchema.Example("\"mickkc\"")
    val username: String,

    @JsonSchema.Example("\"Mickkc\"")
    @JsonSchema.Description("If null, display the username instead.")
    val displayName: String?,

    @JsonSchema.Example("\"I am Mickkc.\"")
    val description: String,

    @JsonSchema.Example("1")
    val imageId: Long?,

    @JsonSchema.Example("\"mickkc@example.com\"")
    val email: String?,

    @JsonSchema.Example("false")
    val emailVerified: Boolean,

    @JsonSchema.Example("true")
    val isActive: Boolean,

    @JsonSchema.Example("true")
    val isAdmin: Boolean,

    @JsonSchema.Example("1782511260")
    val lastLogin: Long?,

    @JsonSchema.Example("1782511260")
    val createdAt: Long,

    @JsonSchema.Example("1782511260")
    val updatedAt: Long?
)