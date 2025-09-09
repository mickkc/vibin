package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.db.tracks.TrackTable
import wtf.ndu.vibin.dto.IdNameDto
import wtf.ndu.vibin.dto.tracks.MinimalTrackDto
import wtf.ndu.vibin.dto.tracks.TrackDto
import wtf.ndu.vibin.dto.tracks.TrackEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.parsing.TrackMetadata
import wtf.ndu.vibin.processing.ThumbnailProcessor
import wtf.ndu.vibin.search.SearchQueryBuilder
import wtf.ndu.vibin.utils.ChecksumUtil
import wtf.ndu.vibin.utils.DateTimeUtils
import wtf.ndu.vibin.utils.PathUtils
import java.io.File

object TrackRepo {

    fun count(): Long = transaction {
        return@transaction TrackEntity.count()
    }

    fun getByChecksum(checksum: String): TrackEntity? = transaction {
        return@transaction TrackEntity.find { TrackTable.checksum eq checksum }.firstOrNull()
    }

    fun getById(id: Long): TrackEntity? = transaction {
        return@transaction TrackEntity.findById(id)
    }

    fun getCover(track: TrackEntity): ImageEntity? = transaction {
        return@transaction track.cover
    }

    fun createTrack(file: File, metadata: TrackMetadata, album: AlbumEntity, artists: List<ArtistEntity>?, checksum: String? = null): TrackEntity = transaction {
        return@transaction TrackEntity.new {
            this.title = metadata.title
            this.trackNumber = metadata.trackNumber
            this.trackCount = metadata.trackCount
            this.discNumber = metadata.discNumber
            this.discCount = metadata.discCount
            this.year = metadata.year
            this.duration = metadata.durationMs
            this.comment = metadata.comment ?: ""
            this.explicit = metadata.explicit ?: false
            this.path = PathUtils.getTrackPathFromFile(file)
            this.checksum = checksum ?: ChecksumUtil.getChecksum(file)

            this.album = album
            this.artists = SizedCollection(artists ?: emptyList())
        }
    }

    fun update(track: TrackEntity, block: TrackEntity.() -> Unit): TrackEntity = transaction {
        val updated = track.apply(block)
        updated.updatedAt = DateTimeUtils.now()
        return@transaction updated
    }

    fun update(trackId: Long, editDto: TrackEditDto): TrackEntity? = transaction {

        val track = TrackEntity.findById(trackId) ?: return@transaction null

        editDto.title?.takeIf { it.isNotBlank() }?.let { track.title = it }
        editDto.explicit?.let { track.explicit = it }

        editDto.trackNumber?.let { track.trackNumber = it.takeIf { it > 0 } }
        editDto.trackCount?.let { track.trackCount = it.takeIf { it > 0 } }
        editDto.discNumber?.let { track.discNumber = it.takeIf { it > 0 } }
        editDto.discCount?.let { track.discCount = it.takeIf { it > 0 } }

        editDto.year?.let { track.year = it.takeIf { it > 0 } }
        editDto.comment?.let { track.comment = it }

        editDto.imageUrl?.let { imageUrl ->
            val imageData = runBlocking { Parser.downloadCoverImage(imageUrl) } ?: return@let
            val image = ThumbnailProcessor.getImage(imageData, ThumbnailProcessor.ThumbnailType.TRACK, track.id.value.toString())
            image?.let { track.cover = it}
        }

        editDto.albumId?.let { albumId ->
            if (editDto.albumId != track.album.id.value) {
                val album = AlbumEntity.findById(albumId)
                album?.let { track.album = it }
            }
        }

        editDto.artistIds?.let { artistIds ->
            val artists = ArtistEntity.find { ArtistTable.id inList artistIds }.toList()
            track.artists = SizedCollection(artists)
        }

        track.updatedAt = DateTimeUtils.now()

        return@transaction track
    }

    fun getAll(page: Int, pageSize: Int): Pair<List<TrackEntity>, Long> = transaction {
        val tracks = TrackEntity.all()
        val count = tracks.count()
        val results = tracks
            .orderBy(TrackTable.title to SortOrder.ASC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .toList()
        return@transaction results to count
    }

    /**
     * Searches for tracks based on the provided query string.
     *
     * @param query The search query string.
     * @param advanced If true, uses advanced search parsing; otherwise, performs a simple case-insensitive title search.
     * @param page The page number for pagination (1-based).
     * @param pageSize The number of items per page.
     * @return A list of [TrackEntity] matching the search criteria.
     */
    fun getSearched(query: String, advanced: Boolean, page: Int, pageSize: Int): Pair<List<TrackEntity>, Long> = transaction {
        val tracks = TrackEntity.find { buildQuery(query, advanced) }
        val count = tracks.count()
        val results = tracks
            .orderBy(TrackTable.title to SortOrder.ASC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .toList()
        return@transaction results to count
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

    fun getAllFromAlbum(albumId: Long): List<TrackEntity> = transaction {
        return@transaction TrackEntity.find { TrackTable.albumId eq albumId }.toList()
    }

    fun toDto(trackEntity: TrackEntity): TrackDto = transaction {
        return@transaction toDtoInternal(trackEntity)
    }

    fun toDto(trackEntities: List<TrackEntity>): List<TrackDto> = transaction {
        return@transaction trackEntities.map { toDtoInternal(it) }
    }

    fun delete(track: TrackEntity) = transaction {
        track.cover?.delete()
        track.delete()
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
            cover = trackEntity.cover?.let { ImageRepo.toDto(it) },
            path = trackEntity.path,
            checksum = trackEntity.checksum,
            tags = TagRepo.toDto(trackEntity.tags.toList()),
            uploader = trackEntity.uploader?.let { UserRepo.toDto(it) },
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

    private fun toMinimalDtoInternal(trackEntity: TrackEntity): MinimalTrackDto {
        return MinimalTrackDto(
            id = trackEntity.id.value,
            title = trackEntity.title,
            album = IdNameDto(id = trackEntity.album.id.value, trackEntity.album.title),
            artists = trackEntity.artists.map { IdNameDto(it.id.value, it.name) },
            duration = trackEntity.duration,
            cover = trackEntity.cover?.let { ImageRepo.toDto(it) },
            uploader = trackEntity.uploader?.let { IdNameDto(it.id.value, it.displayName ?: it.username) }
        )
    }
}