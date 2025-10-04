package wtf.ndu.vibin.settings

import kotlinx.serialization.json.Json
import wtf.ndu.vibin.parsing.Parser

private val json = Json {}

object ArtistNameDelimiters : Setting<List<String>>(
    key = "artist_name_delimiters",
    parser = { value -> json.decodeFromString<List<String>>(value) },
    serializer = { value -> json.encodeToString(value) },
    defaultValue = listOf(", ", " & ", " feat. ", " ft. ", "; ")
)

object MetadataLanguage : Setting<String>(
    key = "metadata_language",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "en"
)

object SpotifyClientId : Setting<String>(
    key = "spotify_client_id",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = ""
)

object SpotifyClientSecret : Setting<String>(
    key = "spotify_client_secret",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = ""
)

object PrimaryMetadataSource : Setting<String>(
    key = "primary_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "Metadata",
    generator = { Parser.getFileProviders() }
)

object FallbackMetadataSource : Setting<String>(
    key = "fallback_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "None",
    generator = { Parser.getFileProviders() }
)

object AddGenreAsTag : Setting<Boolean>(
    key = "add_genre_as_tag",
    parser = { value -> value.toBoolean() },
    serializer = { value -> value.toString() },
    defaultValue = true
)

object PageSize : Setting<Int>(
    key = "page_size",
    parser = { value -> value.toIntOrNull() ?: 50 },
    serializer = { value -> value.toString() },
    defaultValue = 50
)