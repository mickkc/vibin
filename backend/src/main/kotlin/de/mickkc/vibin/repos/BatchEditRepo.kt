package de.mickkc.vibin.repos

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

                val trackChanges = mutableListOf<BatchEditResultDtoChange?>()

                edit.actions.forEach { action ->
                    when (action.actionType) {
                        BatchEditActionType.ADD_TAGS -> {
                            trackChanges += batchEditTags(track, action.data["ids"], edit.dryRun, false)
                        }
                        BatchEditActionType.REMOVE_TAGS -> {
                            trackChanges += batchEditTags(track, action.data["ids"], edit.dryRun, true)
                        }
                        BatchEditActionType.ADD_TO_PLAYLIST -> {
                            trackChanges += batchEditPlaylist(track, action.data["id"], edit.dryRun, userId, false)
                        }
                        BatchEditActionType.REMOVE_FROM_PLAYLIST -> {
                            trackChanges += batchEditPlaylist(track, action.data["id"], edit.dryRun, userId, true)
                        }
                        BatchEditActionType.TITLE -> {
                            trackChanges += batchEditRename(track, action.data["from"], action.data["to"], edit.dryRun)
                        }
                        BatchEditActionType.CHANGE_COMMENT -> {
                            trackChanges += batchEditComment(track, action.data["from"], action.data["to"], edit.dryRun)
                        }
                        BatchEditActionType.REMOVE_LISTEN_DURATION -> {
                            trackChanges += batchEditRemoveListenDuration(track, edit.dryRun)
                        }
                        BatchEditActionType.REGENERATE_FILE_METADATA -> {
                            trackChanges += batchRescanFileMetadata(track, edit.dryRun)
                        }
                        BatchEditActionType.CHANGE_ALBUM -> {
                            trackChanges += batchEditAlbum(track, action.data["id"], edit.dryRun)
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

    fun batchEditTags(track: TrackEntity, tagIds: Any?, dryRun: Boolean, remove: Boolean): BatchEditResultDtoChange? {
        val currentTags = track.tags.toMutableList()
        val tagStrBefore = currentTags.joinToString(", ") { it.name }

        if (tagIds == null || tagIds !is List<*>) {
            throw BatchEditException("Invalid data: Tag ids is not a List.")
        }

        var modifiedTags = false

        tagIds.forEach { tagId ->
            val tagIdLong = (tagId as? Number)?.toLong()
                ?: throw BatchEditException("Invalid data: Tag id is not a number.")

            if (remove) {
                if (currentTags.any { it.id.value == tagIdLong }) {
                    currentTags.removeAll { it.id.value == tagIdLong }
                    modifiedTags = true
                }
            } else {
                if (!currentTags.any { it.id.value == tagIdLong }) {
                    val tag = TagRepo.getById(tagIdLong)
                        ?: throw BatchEditException("Invalid data: Tag with id $tagIdLong not found.")
                    currentTags += tag
                    modifiedTags = true
                }
            }
        }

        if (modifiedTags) {
            if (!dryRun) {
                track.tags = SizedCollection(currentTags)
            }

            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.TAGS,
                before = tagStrBefore,
                after = currentTags.joinToString(", ") { it.name }
            )
        }

        return null
    }

    fun batchEditPlaylist(track: TrackEntity, playlistId: Any?, dryRun: Boolean, userId: Long, remove: Boolean): BatchEditResultDtoChange? {
        val currentPlaylists = PlaylistTrackRepo.getPlaylistsWithTrack(track, userId).toMutableList()
        val playlistStrBefore = currentPlaylists.joinToString(", ") { it.name }

        val playlistIdLong = (playlistId as? Number)?.toLong()
            ?: throw BatchEditException("Invalid data: Playlist id is not a number.")

        var modifiedPlaylist = false

        if (remove) {
            val matchingPlaylist = currentPlaylists.find { it.id.value == playlistIdLong }
            if (matchingPlaylist != null) {
                currentPlaylists.remove(matchingPlaylist)
                if (!dryRun) {
                    PlaylistTrackRepo.removeTrackFromPlaylist(matchingPlaylist, track)
                }
                modifiedPlaylist = true
            }
        }
        else {
            if (!currentPlaylists.any { it.id.value == playlistIdLong }) {
                val playlist = PlaylistRepo.getById(playlistIdLong, userId)
                    ?: throw BatchEditException("Invalid data: Playlist with id $playlistIdLong not found.")
                val user = UserRepo.getById(userId)
                    ?: throw BatchEditException("Invalid data: User with id $userId not found.")
                currentPlaylists += playlist
                if (!dryRun) {
                    PlaylistTrackRepo.addTrackToPlaylist(playlist, track, user)
                }
                modifiedPlaylist = true
            }
        }

        if (modifiedPlaylist) {
            val playlistStringAfter = currentPlaylists.joinToString(", ") { it.name }
            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.PLAYLIST,
                before = playlistStrBefore,
                after = playlistStringAfter
            )
        }

        return null
    }

    fun batchEditRename(track: TrackEntity, from: Any?, to: Any?, dryRun: Boolean): BatchEditResultDtoChange? {
        if (from !is String)
            throw BatchEditException("Invalid data: 'from' must be a string.")
        if (to !is String)
            throw BatchEditException("Invalid data: 'to' must be a string.")

        val oldTitle = track.title

        val newTitle = Regex(from).replace(track.title, to)

        if (newTitle != oldTitle) {
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

    fun batchEditComment(track: TrackEntity, from: Any?, to: Any?, dryRun: Boolean): BatchEditResultDtoChange? {
        if (from !is String)
            throw BatchEditException("Invalid data: 'from' must be a string.")
        if (to !is String)
            throw BatchEditException("Invalid data: 'to' must be a string.")

        val oldComment = track.comment

        val newComment = Regex(from).replace(track.comment, to)

        if (newComment != oldComment) {
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

    fun batchEditRemoveListenDuration(track: TrackEntity, dryRun: Boolean): BatchEditResultDtoChange? {
        val listenDuration = ListenTrackingRepo.getGlobalListenDurationForTrack(track.id.value)

        if (listenDuration > 0) {
            if (!dryRun) {
                ListenTrackingRepo.deleteAllForTrack(track.id.value)
            }
            return BatchEditResultDtoChange(
                track = toMinimalDtoInternal(track),
                type = BatchEditResultDtoChangeType.LISTEN_DURATION,
                before = "${listenDuration}s",
                after = "0s"
            )
        }

        return null
    }

    fun batchRescanFileMetadata(track: TrackEntity, dryRun: Boolean): BatchEditResultDtoChange? {

        val oldMetadataStr = "Bitrate: ${track.bitrate} kbps, Channels: ${track.channels}, Sample rate: ${track.sampleRate}, Duration: ${track.duration} ms"

        val file = PathUtils.getTrackFileFromPath(track.path)

        val preParseData = PreParser.preParse(file)

        val newBitrate = preParseData.bitrate?.toIntOrNull() ?: track.bitrate
        val newChannelsInt = preParseData.getChannelsInt() ?: track.channels
        val newSampleRate = preParseData.sampleRate?.toIntOrNull() ?: track.sampleRate

        if (
            newBitrate != track.bitrate ||
            newChannelsInt != track.channels ||
            preParseData.durationMs != track.duration ||
            newSampleRate != track.sampleRate
        ) {
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

    fun batchEditAlbum(track: TrackEntity, albumId: Any?, dryRun: Boolean): BatchEditResultDtoChange? {
        val albumIdLong = (albumId as? Number)?.toLong()
            ?: throw BatchEditException("Invalid data: Album id is not a number.")

        val oldAlbum = track.album
        if (oldAlbum.id.value != albumIdLong) {
            val newAlbum = AlbumRepo.getById(albumIdLong)
                ?: throw BatchEditException("Invalid data: Album with id $albumIdLong not found.")
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