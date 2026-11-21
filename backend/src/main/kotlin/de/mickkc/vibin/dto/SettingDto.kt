package de.mickkc.vibin.dto

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class SettingDto<T>(

    @JsonSchema.Example("\"lastfm_api_key\"")
    val key: String,

    @JsonSchema.Example("\"6osQyeK687smeHXyum3b14XEoolPlI5M\"")
    val value: T,

    @JsonSchema.Example("\"^[0-9a-fA-F]{32}$\"")
    val validationRegex: String?
)
