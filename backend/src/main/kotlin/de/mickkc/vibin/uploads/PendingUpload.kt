package de.mickkc.vibin.uploads

import kotlinx.serialization.Serializable

@Serializable
data class PendingUpload(
    val id: String,
    val filePath: String,
    var title: String,
    var album: Long,
    var artists: List<Long>,
    var tags: List<Long>,
    var explicit: Boolean,
    var trackNumber: Int?,
    var trackCount: Int?,
    var discNumber: Int?,
    var discCount: Int?,
    var year: Int?,
    var comment: String,
    var lyrics: String?,
    var uploaderId: Long,
    var lastUpdated: Long
)
