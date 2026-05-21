package de.mickkc.vibin.dto.tracks.batch

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

enum class BatchEditActionType {
    ADD_TAGS,
    REMOVE_TAGS,
    ADD_TO_PLAYLIST,
    REMOVE_FROM_PLAYLIST,
    TITLE,
    CHANGE_COMMENT,
    REMOVE_LISTEN_DURATION,
    REGENERATE_FILE_METADATA,
    CHANGE_ALBUM
}

@Serializable
data class BatchEditAction(
    val actionType: BatchEditActionType,
    val data: Map<String, @Contextual @Serializable Any>
)

@Serializable
data class BatchEditDto(
    val trackIds: List<Long>?,
    val query: String?,

    val actions: List<BatchEditAction>,
    val dryRun: Boolean
)
