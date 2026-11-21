package de.mickkc.vibin.dto.tracks.batch

import io.ktor.openapi.JsonSchema
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

    @JsonSchema.Example("\"ADD_TAGS\"")
    val actionType: BatchEditActionType,

    @JsonSchema.Example("{\"ids\":[1,2,3]}")
    val data: Map<String, @Contextual @Serializable Any>?
)

@Serializable
data class BatchEditDto(

    @JsonSchema.Example("[1,2,3]")
    val trackIds: List<Long>?,

    @JsonSchema.Example("\"y:1987 a:Astley\"")
    val query: String?,

    val actions: List<BatchEditAction>,

    @JsonSchema.Example("true")
    val dryRun: Boolean
)
