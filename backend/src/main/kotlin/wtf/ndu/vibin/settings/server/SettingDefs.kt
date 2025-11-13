package wtf.ndu.vibin.settings.server

import kotlinx.serialization.json.Json
import wtf.ndu.vibin.parsing.MetadataFetchingType

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
    WelcomeTexts,
    ArtistMetadataFetchType,
    ArtistMetadataSource,
    AlbumMetadataFetchType,
    AlbumMetadataSource,
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

object ArtistMetadataFetchType : ServerSetting<MetadataFetchingType>(
    key = "artist_metadata_fetch_type",
    parser = { value -> MetadataFetchingType.valueOf(value) },
    serializer = { value -> value.name },
    defaultValue = MetadataFetchingType.EXACT_MATCH
)

object ArtistMetadataSource : ServerSetting<String>(
    key = "artist_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "Deezer"
)

object AlbumMetadataFetchType : ServerSetting<MetadataFetchingType>(
    key = "album_metadata_fetch_type",
    parser = { value -> MetadataFetchingType.valueOf(value) },
    serializer = { value -> value.name },
    defaultValue = MetadataFetchingType.EXACT_MATCH
)

object AlbumMetadataSource : ServerSetting<String>(
    key = "album_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "Deezer"
)

object LyricsMetadataSource : ServerSetting<String>(
    key = "lyrics_metadata_source",
    parser = { value -> value },
    serializer = { value -> value },
    defaultValue = "LrcLib"
)