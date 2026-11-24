package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable
import de.mickkc.vibin.dto.users.UserDto
import io.ktor.openapi.JsonSchema

@Serializable
data class LoginResultDto (

    @JsonSchema.Example("true")
    val success: Boolean,

    @JsonSchema.Example("\"qeG8LcgvYoa4laMXMuR8yHALVqrbASRczedCRe50yx4llXoboZHQWz6A5RA4jReH\"")
    val token: String,

    val user: UserDto,

    @JsonSchema.Example("[\"change_server_settings\", \"change_own_user_settings\", \"...\"]")
    val permissions: List<String>,

    @JsonSchema.Example("true")
    val totpRequired: Boolean
)