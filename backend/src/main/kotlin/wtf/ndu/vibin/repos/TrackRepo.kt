package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.AlbumEntity
import wtf.ndu.vibin.db.ArtistEntity
import wtf.ndu.vibin.db.ArtistTable
import wtf.ndu.vibin.db.TrackEntity
import wtf.ndu.vibin.db.TrackTable
import wtf.ndu.vibin.dto.TrackDto
import wtf.ndu.vibin.dto.TrackEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.parsing.TrackMetadata
import wtf.ndu.vibin.processing.ThumbnailProcessor
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

    fun getAll(page: Int, pageSize: Int): List<TrackEntity> = transaction {
        return@transaction TrackEntity.all()
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .toList()
    }

    fun toDto(trackEntity: TrackEntity): TrackDto = transaction {
        return@transaction toDtoInternal(trackEntity)
    }

    fun toDto(trackEntities: List<TrackEntity>): List<TrackDto> = transaction {
        return@transaction trackEntities.map { toDtoInternal(it) }
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
}