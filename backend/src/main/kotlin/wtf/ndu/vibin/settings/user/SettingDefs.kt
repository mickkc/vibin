package wtf.ndu.vibin.settings.user

import kotlinx.serialization.json.Json

val userSettings = listOf(
    ShowActivitiesToOthers,
    BlockedArtists,
    BlockedTags
)

object ShowActivitiesToOthers : UserSetting<Boolean>(
    key = "show_activities_to_others",
    parser = { value -> value.toBoolean() },
    serializer = { value -> value.toString() },
    defaultValue = true
)

object BlockedArtists : UserSetting<List<Long>>(
    key = "blocked_artists",
    parser = { value -> Json.decodeFromString<List<Long>>(value) },
    serializer = { value -> Json.encodeToString(value) },
    defaultValue = emptyList()
)

object BlockedTags : UserSetting<List<Long>>(
    key = "blocked_tags",
    parser = { value -> Json.decodeFromString<List<Long>>(value) },
    serializer = { value -> Json.encodeToString(value) },
    defaultValue = emptyList()
)