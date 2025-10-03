package wtf.ndu.vibin.parsing.parsers.spotify

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyData<T>(
    val items: List<T>
)
