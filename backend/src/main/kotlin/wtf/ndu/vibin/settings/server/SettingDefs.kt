package wtf.ndu.vibin.settings.server

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

object WelcomeTexts : ServerSetting<List<String>>(
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

object ArtistNameDelimiters : ServerSetting<List<String>>(
    key = "artist_name_delimiters",
    parser = { value -> Json.decodeFromString<List<String>>(value) },
    serializer = { value -> Json.encodeToString(value) },
    defaultValue = listOf(", ", " & ", " feat. ", " ft. ", "; ")
)

object MetadataLanguage : ServerSetting<String>(
    key = "metadata_language",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "en"
)

object SpotifyClientId : ServerSetting<String>(
    key = "spotify_client_id",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = ""
)

object SpotifyClientSecret : ServerSetting<String>(
    key = "spotify_client_secret",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = ""
)

object LyricFilePathTemplate : ServerSetting<String>(
    key = "lyric_file_path_template",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "{parentPath}{sep}{name}.lrc"
)

object PrimaryMetadataSource : ServerSetting<String>(
    key = "primary_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "Metadata",
)

object FallbackMetadataSource : ServerSetting<String>(
    key = "fallback_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "None",
)

object AddGenreAsTag : ServerSetting<Boolean>(
    key = "add_genre_as_tag",
    parser = { value -> value.toBoolean() },
    serializer = { value -> value.toString() },
    defaultValue = true
)

object UploadPath : ServerSetting<String>(
    key = "upload_path",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "{album}{sep}{artist} - {title}.{ext}"
)