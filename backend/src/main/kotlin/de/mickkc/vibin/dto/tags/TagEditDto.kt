package de.mickkc.vibin.dto.tags

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import kotlinx.serialization.Serializable

@Serializable
data class TagEditDto(
    val name: String,
    val description: String?,
    val importance: Int
) : ValidatableBaseDto {
    override fun validate() {
        if (name.length > 255) invalidParameter("name", "name.length <= 255")
        if (importance !in 0..10) invalidParameter("importance", "0 <= importance <= 10")
    }
}