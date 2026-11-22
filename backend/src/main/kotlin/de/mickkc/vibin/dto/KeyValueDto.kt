package de.mickkc.vibin.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class KeyValueDto<K, V>(
    val key: @Contextual @Serializable K,
    val value: @Contextual @Serializable V
)