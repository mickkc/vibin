package de.mickkc.vibin.scrobbling.lastfm

import kotlinx.serialization.Serializable

@Serializable
data class GetMobileSessionResponse(
    val session: GetMobileSessionResponseSession
)

@Serializable
data class GetMobileSessionResponseSession(
    val name: String,
    val key: String
)