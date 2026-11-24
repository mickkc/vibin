package de.mickkc.vibin.dto.tracks.batch

import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
enum class BatchEditResultDtoChangeType {
    TAGS,
    PLAYLIST,
    NAME,
    COMMENT,
    LISTEN_DURATION,
    FILE_METADATA,
    ALBUM
}

@Serializable
data class BatchEditResultDtoChange(
    val track: MinimalTrackDto,

    @JsonSchema.Example("\"TAGS\"")
    val type: BatchEditResultDtoChangeType,

    @JsonSchema.Example("\"1, 6\"")
    @JsonSchema.Description("A human-readable representation of the field BEFORE processing.")
    val before: String,

    @JsonSchema.Example("\"1, 2, 3, 6\"")
    @JsonSchema.Description("A human-readable representation of the field AFTER processing.")
    val after: String
)

@Serializable
data class BatchEditResultDto(
    @JsonSchema.Example("1")
    val affectedTracks: Int,

    @JsonSchema.Example("1")
    val affecteddFields: Int,

    @JsonSchema.Description("All changed fields")
    val changes: List<BatchEditResultDtoChange>,

    @JsonSchema.Example("false")
    val didWriteToDatabase: Boolean,

    @JsonSchema.Example("\"Some error that occurred while processing.\"")
    val error: String?
) {

    companion object {
        fun error(text: String) = BatchEditResultDto(
            affectedTracks = 0,
            affecteddFields = 0,
            changes = emptyList(),
            didWriteToDatabase = false,
            error = text
        )
    }
}