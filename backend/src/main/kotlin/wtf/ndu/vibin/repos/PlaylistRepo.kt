package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.UserEntity
import wtf.ndu.vibin.db.playlists.PlaylistCollaborator
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.db.playlists.PlaylistTable
import wtf.ndu.vibin.db.playlists.PlaylistTrackTable
import wtf.ndu.vibin.dto.PlaylistDto
import wtf.ndu.vibin.dto.PlaylistEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.utils.DateTimeUtils

object PlaylistRepo {

    fun count(userId: Long): Long = transaction {
        PlaylistEntity.find (createOp(userId)).count()
    }

    fun getAll(page: Int, pageSize: Int, userId: Long): List<PlaylistEntity> = transaction {
        PlaylistEntity.find (createOp(userId))
        .limit(pageSize)
        .offset(((page - 1) * pageSize).toLong())
        .toList()
    }

    fun getById(id: Long, userId: Long): PlaylistEntity? = transaction {
        return@transaction PlaylistEntity.find (createOp(userId) and (PlaylistTable.id eq id)).firstOrNull()
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
     * @param userId The ID of the user creating or updating the playlist.
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
            createdAt = playlistEntity.createdAt,
            updatedAt = playlistEntity.updatedAt
        )
    }
}