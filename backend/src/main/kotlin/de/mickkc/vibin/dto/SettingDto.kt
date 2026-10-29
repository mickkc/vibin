package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class SettingDto<T>(
    val key: String,
    val value: T,
    val validationRegex: String?
)
