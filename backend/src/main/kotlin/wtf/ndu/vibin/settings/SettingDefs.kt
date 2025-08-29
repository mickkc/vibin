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

object PrimaryMetadataSource : Setting<String>(
    key = "primary_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "Metadata",
    generator = { Parser.parsers.keys.toList() }
)

object FallbackMetadataSource : Setting<String>(
    key = "fallback_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "iTunes",
    generator = { Parser.parsers.keys.toList() }
)

object AddGenreAsTag : Setting<Boolean>(
    key = "add_genre_as_tag",
    parser = { value -> value.toBoolean() },
    serializer = { value -> value.toString() },
    defaultValue = true
)