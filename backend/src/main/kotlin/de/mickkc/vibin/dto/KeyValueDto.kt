package de.mickkc.vibin.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class KeyValueDto(
    val key: @Contextual @Serializable Any,
    val value: @Contextual @Serializable Any
)