package wtf.ndu.vibin.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class KeyValueDto(
    val key: String,
    val value: @Contextual @Serializable Any
)