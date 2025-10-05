package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.images.ColorSchemeEntity
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.tags.TrackTagConnection
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
        val track = TrackEntity.new {
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
            this.channels = metadata.fileInfo?.channels?.toIntOrNull()
            this.explicit = metadata.trackInfo.explicit ?: false
            this.path = PathUtils.getTrackPathFromFile(file)
            this.checksum = checksum ?: ChecksumUtil.getChecksum(file)

            this.album = album
            this.artists = SizedCollection(artists ?: emptyList())
        }
        if (metadata.trackInfo.lyrics != null) {
            LyricsRepo.setLyrics(track, metadata.trackInfo.lyrics)
        }
        return@transaction track
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

        track.trackNumber = editDto.trackNumber
        track.trackCount = editDto.trackCount
        track.discNumber = editDto.discNumber
        track.discCount = editDto.discCount
        track.year = editDto.year

        editDto.comment?.let { track.comment = it }

        editDto.imageUrl?.let { imageUrl ->
            val imageData = runBlocking { Parser.downloadCoverImage(imageUrl) } ?: return@let
            val image = ThumbnailProcessor.getImage(imageData, ThumbnailProcessor.ThumbnailType.TRACK, track.id.value.toString())
            image?.let { track.cover = it}
        }

        editDto.albumName?.takeIf { it.isNotBlank() }?.let { albumName ->
            if (albumName != track.album.title) {
                val album = AlbumRepo.getOrCreateAlbum(albumName)
                track.album = album
            }
        }

        editDto.artistNames?.filter { it.isNotBlank() }?.let { artistNames ->
            if (artistNames == track.artists.map { it.name }) return@let
            val artists = artistNames.map { name -> ArtistRepo.getOrCreateArtist(name) }
            track.artists = SizedCollection(artists)
        }

        editDto.tagNames?.filter { it.isNotBlank() }?.let { tagNames ->
            if (tagNames == track.tags.map { it.name }) return@let
            val tags = tagNames.map { name -> TagRepo.getOrCreateTag(name) }
            track.tags = SizedCollection(tags)
        }

        track.updatedAt = DateTimeUtils.now()

        LyricsRepo.setLyrics(track, editDto.lyrics)

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

    fun getRandom(limit: Int): List<TrackEntity> = transaction {
        val ids = TrackTable.select(TrackTable.id).map { it[TrackTable.id].value }
        val randomIds = ids.shuffled().take(limit)
        return@transaction TrackEntity.find { TrackTable.id inList randomIds }.shuffled()
    }

    fun getNewest(limit: Int): List<TrackEntity> = transaction {
        return@transaction TrackEntity.all()
            .orderBy(TrackTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .toList()
    }

    fun getColorScheme(track: TrackEntity): ColorSchemeEntity? = transaction {
        return@transaction track.cover?.colorScheme
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

    fun getSearched(query: String, advanced: Boolean): List<TrackEntity> = transaction {
        return@transaction TrackEntity.find { buildQuery(query, advanced) }
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
            bitrate = trackEntity.bitrate,
            sampleRate = trackEntity.sampleRate,
            channels = trackEntity.channels,
            cover = trackEntity.cover?.let { ImageRepo.toDto(it) },
            path = trackEntity.path,
            checksum = trackEntity.checksum,
            tags = TagRepo.toDto(trackEntity.tags.toList()),
            hasLyrics = LyricsRepo.hasLyrics(trackEntity.id.value),
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