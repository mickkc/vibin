package de.mickkc.vibin.dto.errors

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(
    val type: ErrorDtoType,
    val data: Map<String, String>
) {
    companion object {
        fun fromType(type: ErrorDtoType, vararg data: Pair<String, String>): ErrorDto = ErrorDto(
            type = type,
            data = data.toMap()
        )
    }
}