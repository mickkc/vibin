package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Case
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.db.albums.AlbumTable
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.artists.ArtistTable
import wtf.ndu.vibin.db.artists.TrackArtistConnection
import wtf.ndu.vibin.db.images.ImageEntity
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.db.tracks.TrackTable
import wtf.ndu.vibin.dto.albums.AlbumDataDto
import wtf.ndu.vibin.dto.albums.AlbumDto
import wtf.ndu.vibin.dto.albums.AlbumEditDto
import wtf.ndu.vibin.parsing.Parser
import wtf.ndu.vibin.processing.ThumbnailProcessor

object AlbumRepo {

    /**
     * Retrieves an existing album by title or creates a new one if it doesn't exist.
     *
     * @param title The title of the album to retrieve or create.
     * @return The existing or newly created AlbumEntity.
     */
    fun getOrCreateAlbum(title: String): AlbumEntity = transaction {
        return@transaction AlbumEntity.find { AlbumTable.title.lowerCase() eq title.lowercase() }.firstOrNull()
            ?: AlbumEntity.new { this.title = title }
    }

    fun count(): Long = transaction {
        return@transaction AlbumEntity.count()
    }

    fun getAll(page: Int, pageSize: Int, query: String = ""): Pair<List<AlbumEntity>, Long> = transaction {
        val albums = AlbumEntity.find { AlbumTable.title like "%$query%" }
        val count = albums.count()
        val results = albums
            .orderBy(AlbumTable.title to SortOrder.ASC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .toList()
        return@transaction results to count
    }

    fun autocomplete(query: String, limit: Int): List<String> = transaction {
        AlbumTable.select(AlbumTable.title)
            .where { AlbumTable.title.lowerCase() like "%${query.lowercase()}%" }
            .orderBy(
                (Case()
                    .When(AlbumTable.title.lowerCase() like "${query.lowercase()}%", intLiteral(1))
                    .Else(intLiteral(0))) to SortOrder.DESC,
                AlbumTable.title to SortOrder.ASC
            )
            .limit(limit)
            .map { it[AlbumTable.title] }
    }

    fun getTracks(albumId: Long): List<TrackEntity> {
        return transaction {
            return@transaction TrackEntity.find { TrackTable.albumId eq albumId }
                .orderBy(TrackTable.discNumber to SortOrder.ASC_NULLS_LAST, TrackTable.trackNumber to SortOrder.ASC_NULLS_LAST)
                .toList()
        }
    }

    fun getAlbumCover(album: AlbumEntity): ImageEntity? = transaction {
        if (album.cover != null) return@transaction album.cover
        val trackWithCover = TrackEntity.find { (TrackTable.albumId eq album.id) and (TrackTable.coverId neq null) }
            .orderBy(TrackTable.discNumber to SortOrder.ASC_NULLS_LAST, TrackTable.trackNumber to SortOrder.ASC_NULLS_LAST)
            .firstOrNull()
        return@transaction trackWithCover?.cover
    }

    fun getById(id: Long): AlbumEntity? = transaction {
        return@transaction AlbumEntity.findById(id)
    }

    fun update(albumId: Long, editDto: AlbumEditDto): AlbumEntity? = transaction {
        val album = AlbumEntity.findById(albumId) ?: return@transaction null
        editDto.title?.takeIf { it.isNotBlank() }?.let { album.title = it }
        editDto.coverUrl?.let { url ->
            val cover = album.cover
            album.cover = null
            cover?.delete()
            if (url.isNotEmpty()) {
                val data = runBlocking { Parser.downloadCoverImage(url) }
                if (data != null) {
                    val image = ThumbnailProcessor.getImage(data, ThumbnailProcessor.ThumbnailType.ALBUM, album.id.value.toString())
                    album.cover = image
                }
            }
        }
        return@transaction album
    }

    fun toDto(albumEntity: AlbumEntity): AlbumDto = transaction {
        return@transaction toDtoInternal(albumEntity)
    }

    fun toDto(albumEntities: List<AlbumEntity>): List<AlbumDto> = transaction {
        return@transaction albumEntities.map { toDtoInternal(it) }
    }

    fun toDataDto(albumEntity: AlbumEntity): AlbumDataDto = transaction {
        return@transaction AlbumDataDto(
            album = toDtoInternal(albumEntity),
            tracks = TrackRepo.toDto(getTracks(albumEntity.id.value))
        )
    }

    private fun toDtoInternal(albumEntity: AlbumEntity): AlbumDto = transaction {
        return@transaction AlbumDto(
            id = albumEntity.id.value,
            title = albumEntity.title,
            cover = albumEntity.cover?.let { ImageRepo.toDto(it) },
            artists = ArtistRepo.toDto(getArtistsForAlbum(albumEntity)),
            trackCount = getSongAmountForAlbum(albumEntity),
            createdAt = albumEntity.createdAt,
            updatedAt = albumEntity.updatedAt
        )
    }

    private fun getArtistsForAlbum(album: AlbumEntity): List<ArtistEntity> {
        return ArtistEntity.find {
            ArtistTable.id inSubQuery (
                    TrackArtistConnection
                        .select(TrackArtistConnection.artist)
                        .where { TrackArtistConnection.track inSubQuery (
                                TrackTable
                                    .select(TrackTable.id)
                                    .where { TrackTable.albumId eq album.id })
                        })
        }.toList()
    }

    private fun getSongAmountForAlbum(album: AlbumEntity): Long = TrackEntity.find { TrackTable.albumId eq album.id }.count()
}