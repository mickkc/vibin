package de.mickkc.vibin.repos

import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.playlists.PlaylistEntity
import de.mickkc.vibin.db.tags.TagEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.dto.tracks.batch.*
import de.mickkc.vibin.parsing.parsers.preparser.PreParser
import de.mickkc.vibin.repos.TrackRepo.toMinimalDtoInternal
import de.mickkc.vibin.search.SearchQueryBuilder
import de.mickkc.vibin.utils.PathUtils
import io.ktor.util.logging.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private data class TrackVirtualState(
    var title: String,
    var comment: String,
    var tags: MutableList<TagEntity>,
    var playlists: MutableList<PlaylistEntity>,
    var listenDuration: Long,
    var album: AlbumEntity,
    var bitrate: Int?,
    var channels: Int?,
    var sampleRate: Int?,
    var duration: Long?
)

object BatchEditRepo {

    private val logger: Logger = LoggerFactory.getLogger(BatchEditRepo::class.java)

    fun batchEdit(edit: BatchEditDto, userId: Long): BatchEditResultDto = transaction {
        connection.readOnly = edit.dryRun

        val tracks = if (edit.trackIds != null)
            TrackEntity.find { TrackTable.id inList edit.trackIds }
        else if (edit.query != null)
            TrackEntity.find { SearchQueryBuilder.build(edit.query, userId) }
        else {
            logger.error("No trackIds or query provided to batchEdit.")
            return@transaction BatchEditResultDto.error("No trackIds or query provided.")
        }

        var affectedTracks = 0
        val changes = mutableListOf<BatchEditResultDtoChange?>()

        try {
            tracks.forEach { track ->

                val state = TrackVirtualState(
                    title = track.title,
                    comment = track.comment,
                    tags = track.tags.toMutableList(),
                    playlists = PlaylistTrackRepo.getPlaylistsWithTrack(track, userId).toMutableList(),
                    listenDuration = ListenTrackingRepo.getGlobalListenDurationForTrack(track.id.value),
                    album = track.album,
                    bitrate = track.bitrate,
                    channels = track.channels,
                    sampleRate = track.sampleRate,
                    duration = track.duration
                )

                val trackChanges = mutableListOf<BatchEditResultDtoChange?>()

                edit.actions.forEach { action ->
                    when (action.actionType) {
                        BatchEditActionType.ADD_TAGS -> {
                            trackChanges += batchEditTags(track, state, action.data["ids"], edit.dryRun, false)
                        }
                        BatchEditActionType.REMOVE_TAGS -> {
                            trackChanges += batchEditTags(track, state, action.data["ids"], edit.dryRun, true)
                        }
                        BatchEditActionType.ADD_TO_PLAYLIST -> {
                            trackChanges += batchEditPlaylist(track, state, action.data["id"], edit.dryRun, userId, false)
                        }
                        BatchEditActionType.REMOVE_FROM_PLAYLIST -> {
                            trackChanges += batchEditPlaylist(track, state, action.data["id"], edit.dryRun, userId, true)
                        }
                        BatchEditActionType.TITLE -> {
                            trackChanges += batchEditRename(track, state, action.data["from"], action.data["to"], edit.dryRun)
                        }
                        BatchEditActionType.CHANGE_COMMENT -> {
                            trackChanges += batchEditComment(track, state, action.data["from"], action.data["to"], edit.dryRun)
                        }
                        BatchEditActionType.REMOVE_LISTEN_DURATION -> {
                            trackChanges += batchEditRemoveListenDuration(track, state, edit.dryRun)
                        }
                        BatchEditActionType.REGENERATE_FILE_METADATA -> {
                            trackChanges += batchRescanFileMetadata(track, state, edit.dryRun)
                        }
                        BatchEditActionType.CHANGE_ALBUM -> {
                            trackChanges += batchEditAlbum(track, state, action.data["id"], edit.dryRun)
                        }
                    }
                }

                if (trackChanges.any { it != null })
                    affectedTracks++

                changes += trackChanges
            }
        }
        catch (e: Exception) {
            rollback()
            logger.error(e)
            return@transaction BatchEditResultDto.error(e.message ?: e.javaClass.simpleName)
        }

        val actualChanges = changes.filterNotNull()

        return@transaction BatchEditResultDto(
            affectedTracks = affectedTracks,
            affecteddFields = actualChanges.size,
            changes = actualChanges,
            didWriteToDatabase = !edit.dryRun && affectedTracks > 0,
            error = null
        )
    }

    private fun batchEditTags(track: TrackEntity, state: TrackVirtualState, tagIds: Any?, dryRun: Boolean, remove: Boolean): BatchEditResultDtoChange? {
        val tagStrBefore = state.tags.joinToString(", ") { it.name }

        if (tagIds == null || tagIds !is List<*>) {
            throw BatchEditException("Invalid data: Tag ids is not a List.")
        }

        var modifiedTags = false

        tagIds.forEach { tagId ->
            val tagIdLong = (tagId as? Number)?.toLong()
                ?: throw BatchEditException("Invalid data: Tag id is not a number.")

            if (remove) {
                if (state.tags.any { it.id.value == tagIdLong }) {
                    state.tags.removeAll { it.id.value == tagIdLong }
                    modifiedTags = true
                }
            } else {
                if (!state.tags.any { it.id.value == tagIdLong }) {
                    val tag = TagRepo.getById(tagIdLong)
                        ?: throw BatchEditException("Invalid data: Tag with id $tagIdLong not found.")
                    state.tags += tag
                    modifiedTags = true
                }
            }
        }

        if (modifiedTags) {
            if (!dryRun) {
                track.tags = SizedCollection(state.tags)
            }

            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.TAGS,
                before = tagStrBefore,
                after = state.tags.joinToString(", ") { it.name }
            )
        }

        return null
    }

    private fun batchEditPlaylist(track: TrackEntity, state: TrackVirtualState, playlistId: Any?, dryRun: Boolean, userId: Long, remove: Boolean): BatchEditResultDtoChange? {
        val playlistStrBefore = state.playlists.joinToString(", ") { it.name }

        val playlistIdLong = (playlistId as? Number)?.toLong()
            ?: throw BatchEditException("Invalid data: Playlist id is not a number.")

        var modifiedPlaylist = false

        if (remove) {
            val matchingPlaylist = state.playlists.find { it.id.value == playlistIdLong }
            if (matchingPlaylist != null) {
                state.playlists.remove(matchingPlaylist)
                if (!dryRun) {
                    PlaylistTrackRepo.removeTrackFromPlaylist(matchingPlaylist, track)
                }
                modifiedPlaylist = true
            }
        }
        else {
            if (!state.playlists.any { it.id.value == playlistIdLong }) {
                val playlist = PlaylistRepo.getById(playlistIdLong, userId)
                    ?: throw BatchEditException("Invalid data: Playlist with id $playlistIdLong not found.")
                val user = UserRepo.getById(userId)
                    ?: throw BatchEditException("Invalid data: User with id $userId not found.")
                state.playlists += playlist
                if (!dryRun) {
                    PlaylistTrackRepo.addTrackToPlaylist(playlist, track, user)
                }
                modifiedPlaylist = true
            }
        }

        if (modifiedPlaylist) {
            val playlistStringAfter = state.playlists.joinToString(", ") { it.name }
            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.PLAYLIST,
                before = playlistStrBefore,
                after = playlistStringAfter
            )
        }

        return null
    }

    private fun batchEditRename(track: TrackEntity, state: TrackVirtualState, from: Any?, to: Any?, dryRun: Boolean): BatchEditResultDtoChange? {
        if (from !is String)
            throw BatchEditException("Invalid data: 'from' must be a string.")
        if (to !is String)
            throw BatchEditException("Invalid data: 'to' must be a string.")

        val oldTitle = state.title

        val newTitle = Regex(from).replace(state.title, to)

        if (newTitle != oldTitle) {
            state.title = newTitle
            if (!dryRun) {
                track.title = newTitle
            }
            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.NAME,
                before = oldTitle,
                after = newTitle
            )
        }

        return null
    }

    private fun batchEditComment(track: TrackEntity, state: TrackVirtualState, from: Any?, to: Any?, dryRun: Boolean): BatchEditResultDtoChange? {
        if (from !is String)
            throw BatchEditException("Invalid data: 'from' must be a string.")
        if (to !is String)
            throw BatchEditException("Invalid data: 'to' must be a string.")

        val oldComment = state.comment

        val newComment = Regex(from).replace(state.comment, to)

        if (newComment != oldComment) {
            state.comment = newComment
            if (!dryRun) {
                track.comment = newComment
            }
            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.COMMENT,
                before = oldComment,
                after = newComment
            )
        }

        return null
    }

    private fun batchEditRemoveListenDuration(track: TrackEntity, state: TrackVirtualState, dryRun: Boolean): BatchEditResultDtoChange? {
        if (state.listenDuration > 0) {
            val oldListenDuration = state.listenDuration
            state.listenDuration = 0
            if (!dryRun) {
                ListenTrackingRepo.deleteAllForTrack(track.id.value)
            }
            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.LISTEN_DURATION,
                before = "${oldListenDuration}s",
                after = "0s"
            )
        }

        return null
    }

    private fun batchRescanFileMetadata(track: TrackEntity, state: TrackVirtualState, dryRun: Boolean): BatchEditResultDtoChange? {

        val oldMetadataStr = "Bitrate: ${state.bitrate} kbps, Channels: ${state.channels}, Sample rate: ${state.sampleRate}, Duration: ${state.duration} ms"

        val file = PathUtils.getTrackFileFromPath(track.path)

        val preParseData = PreParser.preParse(file)

        val newBitrate = preParseData.bitrate?.toIntOrNull() ?: state.bitrate
        val newChannelsInt = preParseData.getChannelsInt() ?: state.channels
        val newSampleRate = preParseData.sampleRate?.toIntOrNull() ?: state.sampleRate

        if (
            newBitrate != state.bitrate ||
            newChannelsInt != state.channels ||
            preParseData.durationMs != state.duration ||
            newSampleRate != state.sampleRate
        ) {
            state.bitrate = newBitrate
            state.channels = newChannelsInt
            state.duration = preParseData.durationMs
            state.sampleRate = newSampleRate

            if (!dryRun) {
                track.bitrate = newBitrate
                track.channels = newChannelsInt
                track.duration = preParseData.durationMs
                track.sampleRate = newSampleRate
            }

            val newMetadataStr = "Bitrate: $newBitrate kbps, Channels: $newChannelsInt, Sample rate: $newSampleRate, Duration: ${preParseData.durationMs} ms"

            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.FILE_METADATA,
                before = oldMetadataStr,
                after = newMetadataStr
            )
        }

        return null
    }

    private fun batchEditAlbum(track: TrackEntity, state: TrackVirtualState, albumId: Any?, dryRun: Boolean): BatchEditResultDtoChange? {
        val albumIdLong = (albumId as? Number)?.toLong()
            ?: throw BatchEditException("Invalid data: Album id is not a number.")

        val oldAlbum = state.album
        if (oldAlbum.id.value != albumIdLong) {
            val newAlbum = AlbumRepo.getById(albumIdLong)
                ?: throw BatchEditException("Invalid data: Album with id $albumIdLong not found.")
            state.album = newAlbum
            if (!dryRun) {
                track.album = newAlbum
            }

            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.ALBUM,
                before = oldAlbum.title,
                after = newAlbum.title
            )
        }

        return null
    }
}

class BatchEditException(message: String) : Exception(message)