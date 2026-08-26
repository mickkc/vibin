package de.mickkc.vibin.dto.albums

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import kotlinx.serialization.Serializable

@Serializable
data class AlbumEditDto(
    val title: String?,
    val coverUrl: String?,
    val description: String?,
    val year: Int?,
    val isSingle: Boolean?,
) : ValidatableBaseDto {
    override fun validate() {
        if (title != null && title.length > 255) invalidParameter("title", "title.length < 255")
    }
}
