package de.mickkc.vibin.repos

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.UserEntity
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.artists.TrackArtistConnection
import de.mickkc.vibin.db.images.ColorSchemeEntity
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.tags.TrackTagConnection
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.dto.IdNameDto
import de.mickkc.vibin.dto.tracks.MinimalTrackDto
import de.mickkc.vibin.dto.tracks.TrackDto
import de.mickkc.vibin.dto.tracks.TrackEditDto
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.parsing.TrackMetadata
import de.mickkc.vibin.parsing.parsers.PreparseData
import de.mickkc.vibin.routes.PaginatedSearchParams
import de.mickkc.vibin.search.SearchQueryBuilder
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.user.BlockedArtists
import de.mickkc.vibin.settings.user.BlockedTags
import de.mickkc.vibin.uploads.PendingUpload
import de.mickkc.vibin.utils.ChecksumUtil
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.utils.PathUtils
import java.io.File

object TrackRepo {

    fun count(): Long = transaction {
        return@transaction TrackEntity.count()
    }

    fun getTotalRuntimeSeconds(): Long = transaction {
        val tracksWithDuration = TrackEntity.find { TrackTable.duration.isNotNull() }
        return@transaction tracksWithDuration.sumOf { it.duration!! / 1000 }
    }

    fun getByChecksum(checksum: String): TrackEntity? = transaction {
        return@transaction TrackEntity.find { TrackTable.checksum eq checksum }.firstOrNull()
    }

    fun getById(id: Long): TrackEntity? = transaction {
        return@transaction TrackEntity.findById(id)
    }

    fun getArtists(track: TrackEntity): List<ArtistEntity> = transaction {
        return@transaction track.artists.toList()
    }

    fun getByArtistId(artistId: Long, userId: Long? = null): List<TrackEntity> = transaction {
        TrackEntity.find {
            notBlockedByUserOp(userId) and (TrackTable.id inSubQuery
                TrackArtistConnection.select(TrackArtistConnection.track).where { TrackArtistConnection.artist eq artistId }
            )
        }.toList()
    }

    fun getCover(track: TrackEntity): ImageEntity? = transaction {
        if (track.cover != null) return@transaction track.cover
        return@transaction track.album.cover
    }

    suspend fun createTrack(file: File, metadata: TrackMetadata, checksum: String? = null, uploader: UserEntity? = null, cover: ImageEntity? = null): TrackEntity {

        val album = metadata.trackInfo.album
            ?.let { AlbumRepo.getOrCreateAlbum(it, metadata.trackInfo.artists?.firstOrNull()) }
            ?: AlbumRepo.getUnknownAlbum()

        val artists = SizedCollection(metadata.trackInfo.artists?.map { ArtistRepo.getOrCreateArtist(it) } ?: emptyList())

        val tags = SizedCollection(metadata.trackInfo.tags?.map { TagRepo.getOrCreateTag(it) } ?: emptyList())

        val track = transaction {
            TrackEntity.new {
                this.title = metadata.trackInfo.title
                this.trackNumber = metadata.trackInfo.trackNumber
                this.trackCount = metadata.trackInfo.trackCount
                this.discNumber = metadata.trackInfo.discNumber
                this.discCount = metadata.trackInfo.discCount
                this.year = metadata.trackInfo.year
                this.duration = metadata.fileInfo?.durationMs
                this.comment = metadata.trackInfo.comment ?: ""
                this.bitrate = metadata.fileInfo?.bitrate?.toIntOrNull()
                this.sampleRate = metadata.fileInfo?.sampleRate?.toIntOrNull()
                this.channels = metadata.fileInfo?.getChannelsInt()
                this.explicit = metadata.trackInfo.explicit ?: false
                this.path = PathUtils.getTrackPathFromFile(file)
                this.checksum = checksum ?: ChecksumUtil.getChecksum(file)
                this.uploader = uploader
                this.cover = cover
                this.album = album
                this.artists = artists
                this.tags = tags
            }
        }

        val lyrics = metadata.trackInfo.lyrics ?: Parser.searchLyricsAuto(metadata)

        if (lyrics != null) {
            LyricsRepo.setLyrics(track, lyrics)
        }
        return track
    }

    fun createTrack(file: File, preparseData: PreparseData, upload: PendingUpload, cover: ImageEntity?): TrackEntity = transaction {
        val track = TrackEntity.new {
            this.title = upload.title
            this.trackNumber = upload.trackNumber
            this.trackCount = upload.trackCount
            this.discNumber = upload.discNumber
            this.discCount = upload.discCount
            this.year = upload.year
            this.duration = preparseData.durationMs
            this.comment = upload.comment
            this.bitrate = preparseData.bitrate?.toIntOrNull()
            this.sampleRate = preparseData.sampleRate?.toIntOrNull()
            this.channels = preparseData.getChannelsInt()
            this.explicit = upload.explicit
            this.path = PathUtils.getTrackPathFromFile(file)
            this.checksum = ChecksumUtil.getChecksum(file)
            this.uploader = UserRepo.getById(upload.uploaderId)
            this.cover = cover

            this.album = AlbumRepo.getById(upload.album) ?: AlbumRepo.getUnknownAlbum()
            this.artists = SizedCollection(upload.artists.mapNotNull { ArtistRepo.getById(it) })
            this.tags = SizedCollection(upload.tags.mapNotNull { TagRepo.getById(it) })
        }

        if (upload.lyrics != null) {
            LyricsRepo.setLyrics(track, upload.lyrics)
        }

        return@transaction track
    }

    fun getTrackIdsWithPath(): List<Pair<Long, String>> = transaction {
        return@transaction TrackTable.select(TrackTable.id, TrackTable.path)
            .map { it[TrackTable.id].value to it[TrackTable.path] }
    }

    fun update(track: TrackEntity, block: TrackEntity.() -> Unit): TrackEntity = transaction {
        val updated = track.apply(block)
        updated.updatedAt = DateTimeUtils.now()
        return@transaction updated
    }

    suspend fun update(trackId: Long, editDto: TrackEditDto): TrackEntity? {

        val track = getById(trackId) ?: return null

        val (changeCover, newCover) = ImageRepo.getUpdatedImage(editDto.imageUrl)

        return transaction {
            editDto.title?.takeIf { it.isNotBlank() }?.let { track.title = it }
            editDto.explicit?.let { track.explicit = it }

            track.trackNumber = editDto.trackNumber
            track.trackCount = editDto.trackCount
            track.discNumber = editDto.discNumber
            track.discCount = editDto.discCount
            track.year = editDto.year

            editDto.comment?.let { track.comment = it }

            if (changeCover) {
                track.cover = newCover
            }

            editDto.album?.let { albumNameId ->
                if (albumNameId != track.album.id.value) {
                    AlbumRepo.getById(albumNameId)?.let {
                        track.album = it
                    }
                }
            }

            editDto.artists?.let { artistNames ->
                if (artistNames == track.artists.map { it.name }) return@let
                val artists = artistNames.mapNotNull { idName -> ArtistRepo.getById(idName) }
                track.artists = SizedCollection(artists)
            }

            editDto.tags?.let { tagIds ->
                if (tagIds == track.tags.map { it.id.value }) return@let
                val tags = tagIds.mapNotNull { id -> TagRepo.getById(id) }
                track.tags = SizedCollection(tags)
            }

            track.updatedAt = DateTimeUtils.now()

            LyricsRepo.setLyrics(track, editDto.lyrics)
            track
        }

    }

    fun getAll(page: Int, pageSize: Int, userId: Long? = null): Pair<List<TrackEntity>, Long> = transaction {
        val tracks = TrackEntity.find(notBlockedByUserOp(userId))
        val count = tracks.count()
        val results = tracks
            .orderBy(TrackTable.title to SortOrder.ASC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .toList()
        return@transaction results to count
    }

    fun getRandom(limit: Int, userId: Long? = null): List<TrackEntity> = transaction {
        val ids = TrackTable
            .select(TrackTable.id)
            .where(notBlockedByUserOp(userId))
            .map { it[TrackTable.id].value }

        val randomIds = ids.shuffled().take(limit)
        return@transaction TrackEntity.find { TrackTable.id inList randomIds }.shuffled()
    }

    fun getNewest(limit: Int, userId: Long? = null): List<TrackEntity> = transaction {
        return@transaction TrackEntity.find(notBlockedByUserOp(userId))
            .orderBy(TrackTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .toList()
    }

    fun getUploadedByUser(fromUserId: Long, requestingUserId: Long? = null): List<TrackEntity> = transaction {
        return@transaction TrackEntity
            .find { notBlockedByUserOp(requestingUserId) and (TrackTable.uploader eq fromUserId) }
            .orderBy(TrackTable.createdAt to SortOrder.DESC)
            .toList()
    }

    fun getColorScheme(track: TrackEntity): ColorSchemeEntity? = transaction {
        return@transaction track.cover?.colorScheme
    }

    /**
     * Searches for tracks based on the provided query string.
     *
     * @param params The paginated search parameters.
     * @param advanced If true, uses advanced search parsing; otherwise, performs a simple case-insensitive title search.
     * @return A list of [TrackEntity] matching the search criteria.
     */
    fun getSearched(params: PaginatedSearchParams, advanced: Boolean, userId: Long? = null): Pair<List<TrackEntity>, Long> = transaction {
        val tracks = TrackEntity.find { notBlockedByUserOp(userId) and buildQuery(params.query, advanced) }
        val count = tracks.count()
        val results = tracks
            .orderBy(TrackTable.title to SortOrder.ASC)
            .limit(params.pageSize)
            .offset(params.offset)
            .toList()
        return@transaction results to count
    }

    fun getSearched(query: String, advanced: Boolean, userId: Long? = null): List<TrackEntity> = transaction {
        return@transaction TrackEntity.find { notBlockedByUserOp(userId) and buildQuery(query, advanced) }
            .toList()
    }

    /**
     * Builds a search query for tracks based on the provided query string.
     *
     * @param query The search query string.
     * @param advanced If true, uses advanced search parsing; otherwise, performs a simple case-insensitive title search.
     * @return An [Op] representing the search condition.
     */
    private fun buildQuery(query: String, advanced: Boolean): Op<Boolean> {
        return if (advanced) {
            SearchQueryBuilder.build(query)
        } else {
            (TrackTable.title.lowerCase() like "%${query.lowercase()}%")
        }
    }

    private fun notBlockedByUserOp(userId: Long?): Op<Boolean> {
        if (userId == null) {
            return Op.TRUE
        }

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

        return TrackTable.id notInSubQuery TrackTagConnection.select(TrackTagConnection.track).where {
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

        return TrackTable.id notInSubQuery TrackArtistConnection.select(TrackArtistConnection.track).where {
            TrackArtistConnection.artist inList blockedArtistIds
        }
    }

    fun getRelated(track: TrackEntity, limit: Int): List<TrackEntity> = transaction {
        val candidates = TrackEntity.find {
            // Exclude the current track
            (TrackTable.id neq track.id.value) and
            // Same album
            ((TrackTable.albumId eq track.album.id.value) or
            // Shared artists
            (TrackTable.id inSubQuery TrackArtistConnection.select(TrackArtistConnection.track).where {
                TrackArtistConnection.artist inList track.artists.map { it.id.value }
            }) or
            // More than half of the same tags
            (TrackTable.id inSubQuery TrackTagConnection.select(TrackTagConnection.track).where {
                TrackTagConnection.tag inList track.tags.map { it.id.value }
            }.groupBy(TrackTagConnection.track).having { TrackTagConnection.tag.count() greater track.tags.count() / 2 }))
        }.toList()
        return@transaction candidates
            .map { it to rateRelatedScore(track, it) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    private fun rateRelatedScore(track: TrackEntity, related: TrackEntity): Int {
        var score = 0
        val sharedArtists = track.artists.intersect(related.artists.toSet()).size
        score += sharedArtists * 3
        if (track.year != null && track.year == related.year) score += 1
        val sharedTags = track.tags.intersect(related.tags.toSet()).size
        score += sharedTags
        val notSharedTags = (track.tags + related.tags).toSet().size - sharedTags
        score -= notSharedTags / 2
        return score
    }

    fun getAllFromAlbum(albumId: Long, userId: Long? = null): List<TrackEntity> = transaction {
        return@transaction TrackEntity
            .find { notBlockedByUserOp(userId) and (TrackTable.albumId eq albumId) }
            .orderBy(TrackTable.discNumber to SortOrder.ASC_NULLS_LAST, TrackTable.trackNumber to SortOrder.ASC_NULLS_LAST)
            .toList()
    }

    fun toDto(trackEntity: TrackEntity): TrackDto = transaction {
        return@transaction toDtoInternal(trackEntity)
    }

    fun toDto(trackEntities: List<TrackEntity>): List<TrackDto> = transaction {
        return@transaction trackEntities.map { toDtoInternal(it) }
    }

    fun delete(track: TrackEntity) = transaction {
        track.delete()
    }

    fun deleteTracksByIds(trackIds: List<Long>) = transaction {
        TrackEntity.find { TrackTable.id inList trackIds }.forEach { it.delete() }
    }

    private fun toDtoInternal(trackEntity: TrackEntity): TrackDto {
        return TrackDto(
            id = trackEntity.id.value,
            title = trackEntity.title,
            album = AlbumRepo.toDto(trackEntity.album),
            artists = ArtistRepo.toDto(trackEntity.artists.toList()),
            explicit = trackEntity.explicit,
            trackNumber = trackEntity.trackNumber,
            trackCount = trackEntity.trackCount,
            discNumber = trackEntity.discNumber,
            discCount = trackEntity.discCount,
            year = trackEntity.year,
            duration = trackEntity.duration,
            comment = trackEntity.comment,
            bitrate = trackEntity.bitrate,
            sampleRate = trackEntity.sampleRate,
            channels = trackEntity.channels,
            path = trackEntity.path,
            tags = trackEntity.tags.map { TagRepo.toDtoInternal(it) },
            hasLyrics = LyricsRepo.hasLyrics(trackEntity.id.value),
            uploader = trackEntity.uploader?.let { UserRepo.toDtoInternal(it) },
            createdAt = trackEntity.createdAt,
            updatedAt = trackEntity.updatedAt
        )
    }

    fun toMinimalDto(trackEntity: TrackEntity): MinimalTrackDto = transaction {
        return@transaction toMinimalDtoInternal(trackEntity)
    }

    fun toMinimalDto(trackEntities: List<TrackEntity>): List<MinimalTrackDto> = transaction {
        return@transaction trackEntities.map { toMinimalDtoInternal(it) }
    }

    internal fun toMinimalDtoInternal(trackEntity: TrackEntity): MinimalTrackDto {
        return MinimalTrackDto(
            id = trackEntity.id.value,
            title = trackEntity.title,
            album = IdNameDto(id = trackEntity.album.id.value, trackEntity.album.title),
            artists = trackEntity.artists.map { IdNameDto(it.id.value, it.name) },
            duration = trackEntity.duration,
            uploader = trackEntity.uploader?.let { IdNameDto(it.id.value, it.displayName ?: it.username) }
        )
    }
}