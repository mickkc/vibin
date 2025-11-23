package de.mickkc.vibin.parsing.parsers.deezer

import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchResponse<T>(
    val data: List<T>,
    val total: Int,
    val next: String?
)
