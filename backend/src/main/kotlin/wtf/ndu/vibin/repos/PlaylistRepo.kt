package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.playlists.PlaylistCollaborator
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.db.playlists.PlaylistTable
import wtf.ndu.vibin.db.playlists.PlaylistTrackTable
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.dto.playlists.PlaylistDataDto
import wtf.ndu.vibin.dto.playlists.PlaylistDto
import wtf.ndu.vibin.dto.playlists.PlaylistEditDto
import wtf.ndu.vibin.dto.playlists.PlaylistTrackDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.permissions.PermissionType
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.utils.DateTimeUtils

object PlaylistRepo {

    private val logger = LoggerFactory.getLogger(PlaylistRepo::class.java)

    fun count(userId: Long): Long = transaction {
        PlaylistEntity.find (createOp(userId)).count()
    }

    fun getAll(page: Int, pageSize: Int, userId: Long): Pair<List<PlaylistEntity>, Long> = transaction {
        val playlists = PlaylistEntity.find (createOp(userId))
        val count = playlists.count()
        val results = playlists
            .orderBy(PlaylistTable.name to SortOrder.DESC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .toList()
        return@transaction results to count
    }

    fun getById(id: Long, userId: Long): PlaylistEntity? = transaction {
        return@transaction PlaylistEntity.find (createOp(userId) and (PlaylistTable.id eq id)).firstOrNull()
    }

    fun checkOwnership(playlistEntity: PlaylistEntity, userId: Long): Boolean = transaction {
        return@transaction playlistEntity.owner.id.value == userId
    }

    fun getByIdIfAllowed(id: Long, userId: Long, perm: PermissionType): PlaylistEntity? = transaction {
        val playlist = PlaylistEntity.findById(id) ?: return@transaction null
        if (playlist.owner.id.value == userId) return@transaction playlist
        if (playlist.collaborators.any { it.id.value == userId }) {
            return@transaction if (PermissionRepo.hasPermissions(userId, listOf(perm))) playlist else null
        }
        return@transaction null
    }

    /**
     * Gets a playlist by ID if the user is a collaborator or the owner.
     *
     * @param id The ID of the playlist to retrieve.
     * @param userId The ID of the user requesting the playlist.
     * @return The [PlaylistEntity] if found and the user has access, otherwise null.
     */
    fun getByIdCollaborative(id: Long, userId: Long): PlaylistEntity? = transaction {
        return@transaction PlaylistEntity.find (createCollaborationOp(userId) and (PlaylistTable.id eq id)).firstOrNull()
    }

    /**
     * Creates a new playlist or updates an existing one.
     * If `playlistId` is provided, it updates the existing playlist; otherwise, it creates a new one.
     *
     * @param user The user creating or updating the playlist.
     * @param editDto The data transfer object containing playlist details.
     * @param playlistId The ID of the playlist to update, or null to create a new one.
     * @return The created or updated [PlaylistEntity], or null if the playlist to update was not found.
     */
    fun createOrUpdatePlaylist(user: UserEntity, editDto: PlaylistEditDto, playlistId: Long?): PlaylistEntity? = transaction {
        val playlist = if (playlistId != null) PlaylistEntity.findById(playlistId) else PlaylistEntity.new {
            this.name = editDto.name
            this.description = editDto.description ?: ""
            this.public = editDto.isPublic ?: false
            this.vibeDef = editDto.vibeDef
            this.owner = user
        }


        if (playlist == null) {
            return@transaction null
        }

        if (playlistId != null) {
            playlist.name = editDto.name
            editDto.description?.let { playlist.description = it }
            editDto.isPublic?.let { playlist.public = it }
            playlist.vibeDef = editDto.vibeDef
        }

        val collaborators = editDto.collaboratorIds?.mapNotNull { UserRepo.getById(it) }?.toList() ?: emptyList()
        if (playlist.collaborators.toList() != collaborators) {
            playlist.collaborators = SizedCollection(collaborators)
        }

        if (editDto.coverImageUrl != null) {
            val image = runBlocking { Parser.downloadCoverImage(editDto.coverImageUrl) }
            if (image != null) {
                val processedImage = ThumbnailProcessor.getImage(image, ThumbnailProcessor.ThumbnailType.PLAYLIST, playlist.id.value.toString())
                playlist.cover = processedImage
            }
        }

        playlist.updatedAt = DateTimeUtils.now()

        return@transaction playlist
    }

    fun deletePlaylist(playlistId: Long) = transaction {
        val playlist = PlaylistEntity.findById(playlistId) ?: return@transaction

        // Delete links to collaborators and tracks
        PlaylistCollaborator.deleteWhere { PlaylistCollaborator.playlist eq playlistId }
        PlaylistTrackTable.deleteWhere { PlaylistTrackTable.playlist eq playlistId }

        // Delete cover image if exists
        playlist.cover?.delete()

        // Finally, delete the playlist
        playlist.delete()
    }

    fun getTracksWithSource(playlist: PlaylistEntity): Map<String, List<TrackEntity>> = transaction {

        val result = mutableMapOf<String, List<TrackEntity>>()

        result["manual"] = playlist.tracks.toList()

        playlist.vibeDef?.takeIf { it.isNotBlank() }?.let { vibeDef ->
            val vibeTracks = TrackRepo.getSearched(vibeDef, true)
            result["vibe"] = vibeTracks
        }

        return@transaction result
    }

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
     * Creates an Op<Boolean> to filter playlists based on visibility to the given user.
     * A playlist is visible if it is public, or if the user is the owner or a collaborator.
     *
     * @param userId The ID of the user for whom to filter playlists.
     */
    private fun createOp(userId: Long): Op<Boolean> {
        return (PlaylistTable.public eq true) or (PlaylistTable.id inSubQuery (PlaylistCollaborator.select(
            PlaylistCollaborator.playlist
        ).where { PlaylistCollaborator.user eq userId }) or (PlaylistTable.owner eq userId))
    }

    /**
     * Creates an Op<Boolean> to filter playlists that the given user can collaborate on.
     * A user can collaborate on a playlist if they are a collaborator or the owner.
     *
     * @param userId The ID of the user for whom to filter collaborative playlists.
     */
    private fun createCollaborationOp(userId: Long): Op<Boolean> {
        return (PlaylistTable.id inSubQuery (PlaylistCollaborator.select(
            PlaylistCollaborator.playlist
        ).where { PlaylistCollaborator.user eq userId }) or (PlaylistTable.owner eq userId))
    }

    fun toDto(playlistEntity: PlaylistEntity): PlaylistDto = transaction {
        return@transaction toDtoInternal(playlistEntity)
    }

    fun toDto(playlistEntities: List<PlaylistEntity>): List<PlaylistDto> = transaction {
        return@transaction playlistEntities.map { toDtoInternal(it) }
    }

    private fun toDtoInternal(playlistEntity: PlaylistEntity): PlaylistDto {
        return PlaylistDto(
            id = playlistEntity.id.value,
            name = playlistEntity.name,
            description = playlistEntity.description,
            cover = playlistEntity.cover?.let { ImageRepo.toDto(it) },
            public = playlistEntity.public,
            hasVibeDef = playlistEntity.vibeDef != null,
            collaborators = UserRepo.toDto(playlistEntity.collaborators.toList()),
            owner = UserRepo.toDto(playlistEntity.owner),
            createdAt = playlistEntity.createdAt,
            updatedAt = playlistEntity.updatedAt
        )
    }

    fun toDataDto(playlist: PlaylistEntity, tracks: Map<String, List<TrackEntity>>): PlaylistDataDto = transaction {
        val tracks = tracks.flatMap {
            val dtos = TrackRepo.toDto(it.value)
            dtos.map { dto -> PlaylistTrackDto(source = it.key, track = dto) }
        }
        return@transaction PlaylistDataDto(
            playlist = toDtoInternal(playlist),
            tracks = tracks
        )
    }
}