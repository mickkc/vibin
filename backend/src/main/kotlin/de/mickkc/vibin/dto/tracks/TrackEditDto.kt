package de.mickkc.vibin.dto.tracks

import kotlinx.serialization.Serializable

@Serializable
data class TrackEditDto(
    val title: String?,
    val explicit: Boolean?,
    val trackNumber: Int?,
    val trackCount: Int?,
    val discNumber: Int?,
    val discCount: Int?,
    val year: Int?,
    val comment: String?,
    val imageUrl: String?,
    val album: Long?,
    val artists: List<Long>?,
    val tags: List<Long>?,
    val lyrics: String?
)