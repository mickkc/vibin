package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.db.playlists.PlaylistTable
import wtf.ndu.vibin.db.playlists.PlaylistTrackTable
import wtf.ndu.vibin.db.tracks.TrackEntity

object PlaylistTrackRepo {

    private val logger = LoggerFactory.getLogger(PlaylistTrackRepo::class.java)

    /**
     * Add a track to the end of the playlist.
     *
     * @param playlist The playlist entity.
     * @param track The track entity to add.
     * @return True if the track was added successfully, false if it was already present.
     */
    fun addTrackToPlaylist(playlist: PlaylistEntity, track: TrackEntity): Boolean = transaction {
        // Check if the track is already in the playlist
        val exists = PlaylistTrackTable.select(PlaylistTrackTable.columns) .where {
            (PlaylistTrackTable.playlist eq playlist.id.value) and
                    (PlaylistTrackTable.track eq track.id.value)
        }.count() > 0

        if (exists) {
            logger.warn("Tried to add track ID ${track.id.value} to playlist ID ${playlist.id.value}, but it is already present.")
            return@transaction false
        }

        // Get the current max position in the playlist
        val maxPosition = PlaylistTrackTable.select(PlaylistTrackTable.position).where {
            PlaylistTrackTable.playlist eq playlist.id.value
        }.maxOfOrNull { it[PlaylistTrackTable.position] } ?: -1

        // Insert the new track at the next position
        PlaylistTrackTable.insert {
            it[this.playlist] = playlist.id.value
            it[this.track] = track.id.value
            it[this.position] = maxPosition + 1
        }

        return@transaction true
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
            (PlaylistTrackTable.playlist eq playlist.id.value) and
                    (PlaylistTrackTable.track eq track.id.value)
        }.map { it[PlaylistTrackTable.position] }.singleOrNull() ?: return@transaction false

        val deleted = PlaylistTrackTable.deleteWhere {
            (PlaylistTrackTable.playlist eq playlist.id.value) and
                    (PlaylistTrackTable.track eq track.id.value)
        }

        if (deleted > 0) {
            // Update positions of remaining tracks
            PlaylistTrackTable.update(
                where = {
                    (PlaylistTrackTable.playlist eq playlist.id.value) and
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
        PlaylistTrackTable.deleteWhere {
            PlaylistTrackTable.track eq track.id.value
        }
    }

    /**
     * Move a track to a new position within the playlist.
     *
     * @param playlist The playlist entity.
     * @param track The track entity to move.
     * @param newPosition The new position index (0-based).
     * @return True if the position was updated successfully, false otherwise.
     */
    fun setPosition(playlist: PlaylistEntity, track: TrackEntity, newPosition: Int): Boolean = transaction {
        val currentPosition = PlaylistTrackTable
            .select(PlaylistTrackTable.position)
            .where {
                (PlaylistTrackTable.playlist eq playlist.id.value) and
                        (PlaylistTrackTable.track eq track.id.value)
            }
            .map { it[PlaylistTrackTable.position] }
            .singleOrNull() ?: return@transaction false

        val trackCount = playlist.tracks.count()
        if (newPosition !in 0..<trackCount) return@transaction false
        if (newPosition == currentPosition) return@transaction true

        if (newPosition < currentPosition) {
            // Moving UP: shift down tracks between newPosition and currentPosition - 1
            PlaylistTrackTable.update(
                where = {
                    (PlaylistTrackTable.playlist eq playlist.id.value) and
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
                    (PlaylistTrackTable.playlist eq playlist.id.value) and
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
                (PlaylistTrackTable.playlist eq playlist.id.value) and
                        (PlaylistTrackTable.track eq track.id.value)
            }
        ) {
            it[PlaylistTrackTable.position] = newPosition
        }

        // Check if all positions are unique and sequential
        val positions = PlaylistTrackTable
            .select(PlaylistTrackTable.position)
            .where { PlaylistTrackTable.playlist eq playlist.id.value }
            .map { it[PlaylistTrackTable.position] }
            .sorted()
        if (positions != (0 until trackCount).toList()) {
            // Something went wrong, rollback
            rollback()
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
        val playlistIds = PlaylistTrackTable.select(PlaylistTrackTable.playlist)
            .where { PlaylistTrackTable.track eq track.id.value }
            .map { it[PlaylistTrackTable.playlist] }
            .distinct()
        return@transaction PlaylistEntity.find { PlaylistTable.id inList playlistIds }.toList()
    }
}