package de.mickkc.vibin.dto.tracks.batch

import de.mickkc.vibin.dto.tracks.MinimalTrackDto
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
    val type: BatchEditResultDtoChangeType,
    val before: String,
    val after: String
)

@Serializable
data class BatchEditResultDto(
    val affectedTracks: Int,
    val affecteddFields: Int,
    val changes: List<BatchEditResultDtoChange>,
    val didWriteToDatabase: Boolean,
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