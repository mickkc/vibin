package de.mickkc.vibin.dto.playlists

import de.mickkc.vibin.dto.ValidatableBaseDto
import de.mickkc.vibin.routes.invalidParameter
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistEditDto (
    val name: String,
    val description: String? = null,
    val isPublic: Boolean? = null,
    val coverImageUrl: String? = null,
    val collaboratorIds: List<Long>? = null,
    val vibedef: String? = null
) : ValidatableBaseDto {
    override fun validate() {
        if (name.length > 255)
            invalidParameter("name", "name.length <= 255")
    }
}