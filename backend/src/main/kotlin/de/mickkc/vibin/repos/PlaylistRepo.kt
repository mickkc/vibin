package de.mickkc.vibin.repos

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.playlists.PlaylistCollaborator
import de.mickkc.vibin.db.playlists.PlaylistEntity
import de.mickkc.vibin.db.playlists.PlaylistTable
import de.mickkc.vibin.dto.playlists.PlaylistDataDto
import de.mickkc.vibin.dto.playlists.PlaylistDto
import de.mickkc.vibin.dto.playlists.PlaylistEditDto
import de.mickkc.vibin.images.ImageCache
import de.mickkc.vibin.permissions.PermissionType
import de.mickkc.vibin.routes.PaginatedSearchParams
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.utils.ImageUtils
import de.mickkc.vibin.utils.PathUtils

object PlaylistRepo {

    private val logger = LoggerFactory.getLogger(PlaylistRepo::class.java)

    fun count() : Long = transaction {
        PlaylistEntity.all().count()
    }

    fun count(userId: Long): Long = transaction {
        PlaylistEntity.find (createOp(userId)).count()
    }

    fun getAll(params: PaginatedSearchParams, userId: Long, onlyOwn: Boolean = false): Pair<List<PlaylistEntity>, Long> = transaction {
        val op = if (onlyOwn) createCollaborationOp(userId) else createOp(userId)
        val playlists = PlaylistEntity.find (op and (PlaylistTable.name.lowerCase() like "%${params.query.lowercase()}%"))
        val count = playlists.count()
        val results = playlists
            .orderBy(PlaylistTable.name to SortOrder.DESC)
            .limit(params.pageSize)
            .offset(params.offset)
            .toList()
        return@transaction results to count
    }

    fun getAllForUser(userId: Long, isSelf: Boolean): List<PlaylistEntity> = transaction {
        val op = if (isSelf) createCollaborationOp(userId) else createPublicCollaborationOp(userId)
        PlaylistEntity.find(op).toList()
    }

    fun getOwnedByUser(userId: Long): List<PlaylistEntity> = transaction {
        PlaylistEntity.find { PlaylistTable.owner eq userId }.toList()
    }

    fun getRandom(limit: Int, userId: Long): List<PlaylistEntity> = transaction {
        val allIds = PlaylistTable
            .select(PlaylistTable.id)
            .where { createCollaborationOp(userId) }
            .map { it[PlaylistTable.id] }
        val randomIds = allIds.shuffled().take(limit)
        return@transaction PlaylistEntity.find { PlaylistTable.id inList randomIds }.shuffled()
    }

    fun getById(id: Long, userId: Long): PlaylistEntity? = transaction {
        return@transaction PlaylistEntity.find (createOp(userId) and (PlaylistTable.id eq id)).firstOrNull()
    }

    fun getByIdPublic(id: Long): PlaylistEntity? = transaction {
        return@transaction PlaylistEntity.find { (PlaylistTable.id eq id) and (PlaylistTable.public eq true) }.firstOrNull()
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
    suspend fun createOrUpdatePlaylist(user: UserEntity, editDto: PlaylistEditDto, playlistId: Long?): PlaylistEntity? {

        val playlist = transaction {
            if (playlistId != null)
                PlaylistEntity.findById(playlistId)
            else PlaylistEntity.new {
                this.name = editDto.name
                this.description = editDto.description ?: ""
                this.public = editDto.isPublic ?: false
                this.vibeDef = editDto.vibedef?.takeIf { it.isNotEmpty() }
                this.owner = user
            }
        }

        if (playlist == null) {
            return null
        }

        val (coverChanged, newCover) = ImageRepo.getUpdatedImage(editDto.coverImageUrl)

        return transaction {
            if (playlistId != null) {
                playlist.name = editDto.name
                editDto.description?.let { playlist.description = it }
                editDto.isPublic?.let { playlist.public = it }
                playlist.vibeDef = editDto.vibedef?.takeIf { it.isNotEmpty() }
            }

            if (editDto.collaboratorIds != null) {
                val collaborators = editDto.collaboratorIds.mapNotNull { UserRepo.getById(it) }.toList()
                if (playlist.collaborators.toList() != collaborators) {
                    playlist.collaborators = SizedCollection(collaborators)
                }
            }

            if (coverChanged) {
                playlist.cover = newCover
            }

            playlist.updatedAt = DateTimeUtils.now()
            playlist
        }
    }

    fun deletePlaylist(playlistId: Long) = transaction {
        val playlist = PlaylistEntity.findById(playlistId) ?: return@transaction
        playlist.delete()
    }



    fun getCoverImageBytes(playlist: PlaylistEntity, quality: Int): ByteArray? = transaction {
        if (playlist.cover != null) {
            val coverFile = ImageCache.getImageFile(playlist.cover!!, quality)
            if (coverFile != null) {
                return@transaction coverFile.readBytes()
            }
        }

        val tracksWithCover = PlaylistTrackRepo.getTracksAsList(playlist, null)
            .filter { it.cover != null }

        if (tracksWithCover.size in 1..3) {
            val firstTrack = tracksWithCover.first()

            val file = ImageCache.getImageFile(firstTrack.cover!!, quality)
            if (file != null) {
                return@transaction file.readBytes()
            }
        }

        if (tracksWithCover.size > 3) {
            val files = tracksWithCover.take(4).map { PathUtils.getThumbnailFileFromPath(it.cover!!.sourcePath) }
            val collage = ImageUtils.generateCollage(files, if (quality <= 0) 1024 else quality, 2)
            if (collage != null) {
                return@transaction collage
            }
        }

        val placeholder = PathUtils.getDefaultImage("playlist", quality)
        return@transaction placeholder?.readBytes()
    }

    /**
     * Creates an Op<Boolean> to filter playlists based on visibility to the given user.
     * A playlist is visible if it is public, or if the user is the owner or a collaborator.
     *
     * @param userId The ID of the user for whom to filter playlists.
     */
    fun createOp(userId: Long): Op<Boolean> {
        return (PlaylistTable.public eq true) or (PlaylistTable.id inSubQuery (PlaylistCollaborator.select(
            PlaylistCollaborator.playlist
        ).where { PlaylistCollaborator.user eq userId }) or (PlaylistTable.owner eq userId))
    }

    /**
     * Creates an Op to filter public playlists that the given user is part of.
     * (collaborator or owner)
     *
     * @param userId The ID of the user for whom to filter public collaborative playlists.
     */
    private fun createPublicCollaborationOp(userId: Long): Op<Boolean> {
        return (PlaylistTable.public eq true) and ((PlaylistTable.id inSubQuery (PlaylistCollaborator.select(
            PlaylistCollaborator.playlist
        ).where { PlaylistCollaborator.user eq userId }) or (PlaylistTable.owner eq userId)))
    }

    /**
     * Creates an Op<Boolean> to filter playlists that the given user can collaborate on.
     * A user can collaborate on a playlist if they are a collaborator or the owner.
     *
     * @param userId The ID of the user for whom to filter collaborative playlists.
     */
    fun createCollaborationOp(userId: Long): Op<Boolean> {
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

    internal fun toDtoInternal(playlistEntity: PlaylistEntity): PlaylistDto {
        return PlaylistDto(
            id = playlistEntity.id.value,
            name = playlistEntity.name,
            description = playlistEntity.description,
            public = playlistEntity.public,
            vibedef = playlistEntity.vibeDef,
            collaborators = playlistEntity.collaborators.map { UserRepo.toDtoInternal(it) },
            owner = UserRepo.toDtoInternal(playlistEntity.owner),
            createdAt = playlistEntity.createdAt,
            updatedAt = playlistEntity.updatedAt
        )
    }

    fun toDataDto(playlist: PlaylistEntity, userId: Long? = null): PlaylistDataDto = transaction {
        val tracks = PlaylistTrackRepo.getTracksAsDtos(playlist, userId)
        return@transaction PlaylistDataDto(
            playlist = toDtoInternal(playlist),
            tracks = tracks
        )
    }
}