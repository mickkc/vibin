package de.mickkc.vibin.dto.playlists

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistEditDto (
    val name: String,
    val description: String? = null,
    val isPublic: Boolean? = null,
    val coverImageUrl: String? = null,
    val collaboratorIds: List<Long>? = null,
    val vibedef: String? = null
)