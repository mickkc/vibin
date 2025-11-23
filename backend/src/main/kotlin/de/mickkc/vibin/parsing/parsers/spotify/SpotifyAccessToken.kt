package de.mickkc.vibin.parsing.parsers.spotify

import kotlinx.serialization.Serializable

@Suppress("PropertyName")
@Serializable
data class SpotifyAccessToken(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)
