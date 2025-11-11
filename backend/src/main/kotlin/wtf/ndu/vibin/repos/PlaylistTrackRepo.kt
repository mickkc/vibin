package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.playlists.PlaylistTrackEntity
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.db.playlists.PlaylistTrackTable
import wtf.ndu.vibin.db.tags.TrackTagConnection
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.dto.IdNameDto
import wtf.ndu.vibin.dto.playlists.PlaylistTrackDto
import wtf.ndu.vibin.settings.Settings
import wtf.ndu.vibin.settings.user.BlockedArtists
import wtf.ndu.vibin.settings.user.BlockedTags

object PlaylistTrackRepo {

    private val logger = LoggerFactory.getLogger(PlaylistTrackRepo::class.java)

    /**
     * Add a track to the end of the playlist.
     *
     * @param playlist The playlist entity.
     * @param track The track entity to add.
     * @return True if the track was added successfully, false if it was already present.
     */
    fun addTrackToPlaylist(playlist: PlaylistEntity, track: TrackEntity, addedBy: UserEntity): PlaylistTrackEntity? = transaction {
        // Check if the track is already in the playlist
        val exists = PlaylistTrackTable.select(PlaylistTrackTable.columns).where {
            (PlaylistTrackTable.playlistId eq playlist.id.value) and
                    (PlaylistTrackTable.trackId eq track.id.value)
        }.count() > 0

        if (exists) {
            logger.warn("Tried to add track ID ${track.id.value} to playlist ID ${playlist.id.value}, but it is already present.")
            return@transaction null
        }

        // Get the current max position in the playlist
        val maxPosition = PlaylistTrackTable.select(PlaylistTrackTable.position).where {
            PlaylistTrackTable.playlistId eq playlist.id.value
        }.maxOfOrNull { it[PlaylistTrackTable.position] } ?: -1

        // Insert the new track at the next position
        return@transaction PlaylistTrackEntity.new {
            this.playlist = playlist
            this.track = track
            this.position = maxPosition + 1
            this.user = addedBy
        }
    }

    /**
     * Remove a track from the playlist.
     *
     * @param playlist The playlist entity.
     * @param track The track entity to remove.
     * @return True if the track was removed successfully, false otherwise.
     */
    fun removeTrackFromPlaylist(playlist: PlaylistEntity, track: TrackEntity): Boolean = transaction {

        val position = PlaylistTrackTable.select(PlaylistTrackTable.position).where {
            (PlaylistTrackTable.playlistId eq playlist.id.value) and
                    (PlaylistTrackTable.trackId eq track.id.value)
        }.map { it[PlaylistTrackTable.position] }.singleOrNull() ?: return@transaction false

        val deleted = PlaylistTrackTable.deleteWhere {
            (PlaylistTrackTable.playlistId eq playlist.id.value) and
                    (PlaylistTrackTable.trackId eq track.id.value)
        }

        if (deleted > 0) {
            // Update positions of remaining tracks
            PlaylistTrackTable.update(
                where = {
                    (PlaylistTrackTable.playlistId eq playlist.id.value) and
                            (PlaylistTrackTable.position greater position)
                }
            ) {
                it[this.position] = this.position - 1
            }
        }
        else {
            logger.warn("Tried to remove track ID ${track.id.value} from playlist ID ${playlist.id.value}, but it was not found.")
        }

        return@transaction deleted > 0
    }

    fun deleteTrackFromAllPlaylists(track: TrackEntity) = transaction {
        PlaylistTrackEntity.find {
            PlaylistTrackTable.trackId eq track.id.value
        }.forEach { it.delete() }
    }

    fun getManuallyAddedTrackCount(playlist: PlaylistEntity): Long = transaction {
        return@transaction PlaylistTrackEntity.find {
            (PlaylistTrackTable.playlistId eq playlist.id.value)
        }.count()
    }

    /**
     * Move a track to a new position within the playlist.
     *
     * @param playlist The playlist entity.
     * @param track The track entity to move.
     * @param afterTrack The track entity after which to place the moved track. If null, the track is moved to the start.
     * @return True if the position was updated successfully, false otherwise.
     */
    fun setPosition(playlist: PlaylistEntity, track: TrackEntity, afterTrack: TrackEntity?): Boolean = transaction {

        val currentPosition = PlaylistTrackTable
            .select(PlaylistTrackTable.position)
            .where {
                (PlaylistTrackTable.playlistId eq playlist.id.value) and
                        (PlaylistTrackTable.trackId eq track.id.value)
            }
            .map { it[PlaylistTrackTable.position] }
            .singleOrNull() ?: return@transaction false.also {
                logger.warn("Tried to move track ID ${track.id.value} in playlist ID ${playlist.id.value}, but it was not found.")
            }

        val newPosition = if (afterTrack == null) {
            0
        } else {
            val afterPosition = PlaylistTrackTable
                .select(PlaylistTrackTable.position)
                .where {
                    (PlaylistTrackTable.playlistId eq playlist.id.value) and
                            (PlaylistTrackTable.trackId eq afterTrack.id.value)
                }
                .map { it[PlaylistTrackTable.position] }
                .singleOrNull() ?: return@transaction false.also {
                    logger.warn("Tried to move track ID ${track.id.value} in playlist ID ${playlist.id.value} after track ID ${afterTrack.id.value}, but the after track was not found.")
                }
            afterPosition + 1
        }

        val trackCount = getManuallyAddedTrackCount(playlist)
        if (newPosition !in 0..<trackCount) return@transaction false.also {
            logger.warn("Tried to move track ID ${track.id.value} in playlist ID ${playlist.id.value} to invalid position $newPosition.")
        }
        if (newPosition == currentPosition) return@transaction true.also {
            logger.info("Track ID ${track.id.value} in playlist ID ${playlist.id.value} is already at position $newPosition, no move needed.")
        }

        if (newPosition < currentPosition) {
            // Moving UP: shift down tracks between newPosition and currentPosition - 1
            PlaylistTrackTable.update(
                where = {
                    (PlaylistTrackTable.playlistId eq playlist.id.value) and
                            (PlaylistTrackTable.position greaterEq newPosition) and
                            (PlaylistTrackTable.position less currentPosition)
                }
            ) {
                it[position] = position + 1
            }
        } else {
            // Moving DOWN: shift up tracks between currentPosition + 1 and newPosition
            PlaylistTrackTable.update(
                where = {
                    (PlaylistTrackTable.playlistId eq playlist.id.value) and
                            (PlaylistTrackTable.position greater currentPosition) and
                            (PlaylistTrackTable.position lessEq newPosition)
                }
            ) {
                it[position] = position - 1
            }
        }

        // Set the track to its new position
        val updated = PlaylistTrackTable.update(
            where = {
                (PlaylistTrackTable.playlistId eq playlist.id.value) and
                        (PlaylistTrackTable.trackId eq track.id.value)
            }
        ) {
            it[PlaylistTrackTable.position] = newPosition
        }

        // Check if all positions are unique and sequential
        val positions = PlaylistTrackTable
            .select(PlaylistTrackTable.position)
            .where { PlaylistTrackTable.playlistId eq playlist.id.value }
            .map { it[PlaylistTrackTable.position] }
            .sorted()
        if (positions != (0 until trackCount.toInt()).toList()) {
            // Something went wrong, rollback
            rollback()
            logger.error("Failed to move track ID ${track.id.value} in playlist ID ${playlist.id.value}: positions are not sequential after update.")
            return@transaction false
        }

        return@transaction updated > 0
    }

    /**
     * Get all playlists that contain the given track.
     *
     * @param track The track entity to search for.
     * @return A list of playlist entities that include the specified track.
     */
    fun getPlaylistsWithTrack(track: TrackEntity): List<PlaylistEntity> = transaction {

        return@transaction PlaylistTrackEntity.find {
            PlaylistTrackTable.trackId eq track.id.value
        }.map { it.playlist }
    }

    fun getTracksFromPlaylist(playlist: PlaylistEntity, userId: Long?): List<PlaylistTrackEntity> = transaction {
        return@transaction PlaylistTrackEntity.find {
            notBlockedByUserOp(userId) and (PlaylistTrackTable.playlistId eq playlist.id.value)
        }.orderBy(PlaylistTrackTable.position to SortOrder.ASC).toList()
    }

    fun notBlockedByUserOp(userId: Long?): Op<Boolean> {
        return notBlockedByTagsOp(userId) and notBlockedByArtistsOp(userId)
    }

    private fun notBlockedByTagsOp(userId: Long?): Op<Boolean> {
        if (userId == null) {
            return Op.TRUE
        }

        val blockedTagIds = Settings.get(BlockedTags, userId)
        if (blockedTagIds.isEmpty()) {
            return Op.TRUE
        }

        return PlaylistTrackTable.trackId notInSubQuery TrackTagConnection.select(TrackTagConnection.track).where {
            TrackTagConnection.tag inList blockedTagIds
        }
    }

    private fun notBlockedByArtistsOp(userId: Long?): Op<Boolean> {
        if (userId == null) {
            return Op.TRUE
        }

        val blockedArtistIds = Settings.get(BlockedArtists, userId)
        if (blockedArtistIds.isEmpty()) {
            return Op.TRUE
        }

        return PlaylistTrackTable.trackId notInSubQuery TrackArtistConnection.select(TrackArtistConnection.track).where {
            TrackArtistConnection.artist inList blockedArtistIds
        }
    }

    fun getTracksAsList(playlist: PlaylistEntity, userId: Long? = null): List<TrackEntity> = transaction {

        var tracks = getTracksFromPlaylist(playlist, userId).map { it.track }

        if (playlist.vibeDef != null) {
            val vibeTracks = TrackRepo.getSearched(playlist.vibeDef!!, true, userId)
            tracks = tracks + vibeTracks.filter { vt ->
                tracks.none { t -> t.id.value == vt.id.value }
            }
        }

        return@transaction tracks
    }

    fun getTracksAsDtos(playlist: PlaylistEntity, userId: Long? = null): List<PlaylistTrackDto> = transaction {

        var result = getTracksFromPlaylist(playlist, userId).map { toDtoInternal(it) }

        if (playlist.vibeDef != null) {
            val vibeTracks = TrackRepo.getSearched(playlist.vibeDef!!, true, userId)
                .filter { vt -> result.none { t -> t.track.id == vt.id.value } }
            result = result + vibeTracks.let { TrackRepo.toMinimalDto(it) }.map {
                PlaylistTrackDto(
                    track = it,
                    position = Int.MAX_VALUE,
                    addedBy = null,
                    addedAt = null
                )
            }
        }

        return@transaction result
    }

    fun toDto(entity: PlaylistTrackEntity): PlaylistTrackDto = transaction {
        return@transaction toDtoInternal(entity)
    }

    fun toDto(entities: List<PlaylistTrackEntity>): List<PlaylistTrackDto> = transaction {
        return@transaction entities.map { toDtoInternal(it) }
    }

    internal fun toDtoInternal(entity: PlaylistTrackEntity): PlaylistTrackDto {
        return PlaylistTrackDto(
            track = TrackRepo.toMinimalDtoInternal(entity.track),
            position = entity.position,
            addedBy = IdNameDto(entity.user.id.value, entity.user.displayName ?: entity.user.username),
            addedAt = entity.addedAt
        )
    }
}