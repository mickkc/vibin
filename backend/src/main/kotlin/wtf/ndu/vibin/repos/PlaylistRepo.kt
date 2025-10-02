package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.transactions.transaction
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
import wtf.ndu.vibin.utils.ImageUtils
import wtf.ndu.vibin.utils.PathUtils

object PlaylistRepo {

    private val logger = LoggerFactory.getLogger(PlaylistRepo::class.java)

    fun count(userId: Long): Long = transaction {
        PlaylistEntity.find (createOp(userId)).count()
    }

    fun getAll(page: Int, pageSize: Int, userId: Long, query: String = ""): Pair<List<PlaylistEntity>, Long> = transaction {
        val playlists = PlaylistEntity.find (createOp(userId) and (PlaylistTable.name like "%$query%"))
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

    fun getCoverImageBytes(playlist: PlaylistEntity, quality: String): ByteArray = transaction {
        if (playlist.cover != null) {
            val path = when(quality) {
                "small" -> playlist.cover!!.smallPath
                "large" -> playlist.cover!!.largePath
                else -> playlist.cover!!.originalPath
            } ?: playlist.cover!!.originalPath

            val file = PathUtils.getThumbnailFileFromPath(path)
            if (file.exists()) {
                return@transaction file.readBytes()
            }
        }

        val tracksWithCover = playlist.tracks.filter { it.cover != null }.toList()

        if (tracksWithCover.size in 1..3) {
            val firstTrack = playlist.tracks.first()
            val cover = firstTrack.cover!!
            val path = when(quality) {
                "small" -> cover.smallPath
                "large" -> cover.largePath
                else -> cover.originalPath
            } ?: cover.originalPath

            val file = PathUtils.getThumbnailFileFromPath(path)
            if (file.exists()) {
                return@transaction file.readBytes()
            }
        }

        if (tracksWithCover.size > 3) {
            val files = tracksWithCover.take(4).map { PathUtils.getThumbnailFileFromPath(it.cover!!.smallPath) }
            val collage = ImageUtils.generateCollage(files, if (quality == "small") 128 else 512, 2)
            if (collage != null) {
                return@transaction collage
            }
        }

        val placeholder = PathUtils.getDefaultImage("playlist", quality)
        return@transaction placeholder.readBytes()
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
            vibedef = playlistEntity.vibeDef,
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