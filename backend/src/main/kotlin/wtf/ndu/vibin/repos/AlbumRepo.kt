package wtf.ndu.vibin.repos

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
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
import wtf.ndu.vibin.routes.PaginatedSearchParams

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

    fun getAll(params: PaginatedSearchParams, showSingles: Boolean = true): Pair<List<AlbumEntity>, Long> = transaction {

        val notSingleOp = if (!showSingles) notSingleOp() else Op.TRUE

        val albums = AlbumEntity.find { (AlbumTable.title.lowerCase() like "%${params.query.lowercase()}%") and notSingleOp }
        val count = albums.count()
        val results = albums
            .orderBy(AlbumTable.title to SortOrder.ASC)
            .limit(params.pageSize)
            .offset(params.offset)
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

    fun getByArtistId(artistId: Long): Map<AlbumEntity, List<TrackEntity>> = transaction {
        val albumIds = TrackTable.select(TrackTable.albumId).where {
            TrackTable.id inSubQuery (
                TrackArtistConnection
                    .select(TrackArtistConnection.track)
                    .where { TrackArtistConnection.artist eq artistId }
            )
        }.map { it[TrackTable.albumId] }

        return@transaction AlbumEntity.find { AlbumTable.id inList albumIds }
            .associateWith { album ->
                TrackEntity.find {
                    (TrackTable.albumId eq album.id) and (TrackTable.id inSubQuery (
                        TrackArtistConnection
                            .select(TrackArtistConnection.track)
                            .where { TrackArtistConnection.artist eq artistId }
                    ))
                }.toList()
            }
    }

    fun update(albumId: Long, editDto: AlbumEditDto): AlbumEntity? = transaction {
        val album = AlbumEntity.findById(albumId) ?: return@transaction null
        editDto.title?.takeIf { it.isNotBlank() }?.let { album.title = it }
        editDto.description?.let { album.description = it }
        album.releaseYear = editDto.year
        album.single = editDto.isSingle
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

    fun notSingleOp(): Op<Boolean> {
        return (AlbumTable.single eq false) or
                (AlbumTable.id inSubQuery TrackTable
                    .select(TrackTable.albumId)
                    .groupBy(TrackTable.albumId)
                    .where { (TrackTable.trackCount greater 1) or (TrackTable.discCount greater 1) }
                )
    }

    fun estimateIsSingle(albumId: Long): Boolean = transaction {
        val trackCounts = TrackTable.select(TrackTable.trackCount).where {
            (TrackTable.albumId eq albumId) and (TrackTable.trackCount neq null)
        }.map { it[TrackTable.trackCount] }
        return@transaction trackCounts.all { it == 1 }
    }

    fun estimateReleaseYear(albumId: Long): Int? = transaction {
        val years = TrackTable.select(TrackTable.year).where {
            (TrackTable.albumId eq albumId) and (TrackTable.year neq null)
        }.mapNotNull { it[TrackTable.year] }
        return@transaction years.maxOrNull()
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
            description = albumEntity.description,
            cover = albumEntity.cover?.let { ImageRepo.toDto(it) },
            artists = ArtistRepo.toDto(getArtistsForAlbum(albumEntity)),
            trackCount = getSongAmountForAlbum(albumEntity),
            year = albumEntity.releaseYear ?: estimateReleaseYear(albumEntity.id.value),
            single = albumEntity.single ?: estimateIsSingle(albumEntity.id.value),
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