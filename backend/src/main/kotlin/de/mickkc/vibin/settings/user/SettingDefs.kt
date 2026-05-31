package de.mickkc.vibin.settings.user

import kotlinx.serialization.json.Json

val userSettings = listOf(
    ShowActivitiesToOthers,
    BlockedArtists,
    BlockedTags,
    RelatedArtistScoreMultiplier,
    RelatedAlbumScoreMultiplier,
    RelatedTagsScoreMultiplier,
    RelatedTagsNegativeScoreMultiplier
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

object RelatedArtistScoreMultiplier : UserSetting<Float>(
    key = "related_artist_score_multiplier",
    parser = { value -> value.toFloat() },
    serializer = { value -> value.toString() },
    defaultValue = 3f
)

object RelatedAlbumScoreMultiplier : UserSetting<Float>(
    key = "related_album_score_multiplier",
    parser = { value -> value.toFloat() },
    serializer = { value -> value.toString() },
    defaultValue = 1f
)

object RelatedTagsScoreMultiplier : UserSetting<Float>(
    key = "related_tags_score_multiplier",
    parser = { value -> value.toFloat() },
    serializer = { value -> value.toString() },
    defaultValue = 1f
)

object RelatedTagsNegativeScoreMultiplier : UserSetting<Float>(
    key = "related_tags_negative_score_multiplier",
    parser = { value -> value.toFloat() },
    serializer = { value -> value.toString() },
    defaultValue = 0.5f
)