package de.mickkc.vibin.dto

import kotlinx.serialization.Serializable

@Serializable
data class UploadResultDto(
    val success: Boolean,
    val didFileAlreadyExist: Boolean = false,
    val id: Long? = null,
)
