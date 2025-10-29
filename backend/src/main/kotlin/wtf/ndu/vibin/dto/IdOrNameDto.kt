package wtf.ndu.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class IdOrNameDto(
    val id: Long? = null,
    val name: String,
    val fallbackName: Boolean = true
) {
    companion object {
        fun nameWithFallback(name: String) = IdOrNameDto(
            id = null,
            name = name,
            fallbackName = true
        )
    }
}
