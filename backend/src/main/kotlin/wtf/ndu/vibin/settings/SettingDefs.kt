package wtf.ndu.vibin.settings

import kotlinx.serialization.json.Json

val serverSettings = listOf(
    ArtistNameDelimiters,
    MetadataLanguage,
    SpotifyClientId,
    SpotifyClientSecret,
    LyricFilePathTemplate,
    PrimaryMetadataSource,
    FallbackMetadataSource,
    AddGenreAsTag,
    UploadPath,
    WelcomeTexts
)

object WelcomeTexts : Setting<List<String>>(
    key = "welcome_texts",
    parser = { value -> Json.decodeFromString<List<String>>(value) },
    serializer = { value -> Json.encodeToString(value) },
    defaultValue = listOf(
        "Welcome to Vibin'!",
        "Hey there!",
        "Ready to vibe?",
        "Ｖｉｂｉｎ＇！",
        "Welcome back!",
    )
)

object ArtistNameDelimiters : Setting<List<String>>(
    key = "artist_name_delimiters",
    parser = { value -> Json.decodeFromString<List<String>>(value) },
    serializer = { value -> Json.encodeToString(value) },
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

object LyricFilePathTemplate : Setting<String>(
    key = "lyric_file_path_template",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "{parentPath}{sep}{name}.lrc"
)

object PrimaryMetadataSource : Setting<String>(
    key = "primary_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "Metadata",
)

object FallbackMetadataSource : Setting<String>(
    key = "fallback_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "None",
)

object AddGenreAsTag : Setting<Boolean>(
    key = "add_genre_as_tag",
    parser = { value -> value.toBoolean() },
    serializer = { value -> value.toString() },
    defaultValue = true
)

object UploadPath : Setting<String>(
    key = "upload_path",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "{album}{sep}{artist} - {title}.{ext}"
)