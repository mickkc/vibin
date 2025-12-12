package de.mickkc.vibin.repos

import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.albums.AlbumTable
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.artists.ArtistTable
import de.mickkc.vibin.db.artists.TrackArtistConnection
import de.mickkc.vibin.db.images.ImageEntity
import de.mickkc.vibin.db.tags.TrackTagConnection
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.dto.albums.AlbumDataDto
import de.mickkc.vibin.dto.albums.AlbumDto
import de.mickkc.vibin.dto.albums.AlbumEditDto
import de.mickkc.vibin.parsing.Parser
import de.mickkc.vibin.processing.ThumbnailProcessor
import de.mickkc.vibin.routes.PaginatedSearchParams
import de.mickkc.vibin.settings.Settings
import de.mickkc.vibin.settings.user.BlockedArtists
import de.mickkc.vibin.settings.user.BlockedTags
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInSubQuery
import org.jetbrains.exposed.sql.transactions.transaction

object AlbumRepo {

    const val UNKNOWN_ALBUM_NAME = "Unknown Album"

    /**
     * Retrieves an existing album by title or creates a new one if it doesn't exist.
     *
     * @param title The title of the album to retrieve or create.
     * @param artistName Optional name of the artist associated with the album for better search results.
     * @return The existing or newly created AlbumEntity.
     */
    suspend fun getOrCreateAlbum(title: String, artistName: String? = null): AlbumEntity {

        if (title == UNKNOWN_ALBUM_NAME) {
            return getUnknownAlbum()
        }

        val album = transaction {
            AlbumEntity.find { AlbumTable.title.lowerCase() eq title.lowercase() }.firstOrNull()
        }

        if (album != null) {
            return album
        }

        val query = if (artistName != null) {
            "$artistName - $title"
        } else {
            title
        }

        val searchResult = Parser.searchAlbumAuto(query)
        val (_, cover) = ImageRepo.getUpdatedImage(searchResult?.coverImageUrl)

        return transaction {
            AlbumEntity.new {
                this.title = title
                this.description = searchResult?.description ?: ""
                this.releaseYear = searchResult?.year
                this.single = searchResult?.isSingle
                this.cover = cover
            }
        }
    }

    fun getByTitle(title: String): AlbumEntity? = transaction {
        return@transaction AlbumEntity.find { AlbumTable.title.lowerCase() eq title.lowercase() }.firstOrNull()
    }

    fun getUnknownAlbum(): AlbumEntity = transaction {
        var album = AlbumEntity.find { AlbumTable.title eq UNKNOWN_ALBUM_NAME }.firstOrNull()
        if (album == null) {
            album = AlbumEntity.new {
                this.title = UNKNOWN_ALBUM_NAME
            }
        }
        return@transaction album
    }

    fun count(): Long = transaction {
        return@transaction AlbumEntity.count()
    }

    fun getAll(params: PaginatedSearchParams, showSingles: Boolean = true, userId: Long? = null): Pair<List<AlbumEntity>, Long> = transaction {

        val notSingleOp = if (!showSingles) notSingleOp() else Op.TRUE

        val albums = AlbumEntity.find { notBlockedByUserOp(userId) and (AlbumTable.title.lowerCase() like "%${params.query.lowercase()}%") and notSingleOp }
        val count = albums.count()
        val results = albums
            .orderBy(AlbumTable.title to SortOrder.ASC)
            .limit(params.pageSize)
            .offset(params.offset)
            .toList()
        return@transaction results to count
    }

    suspend fun create(createDto: AlbumEditDto): AlbumEntity {

        val cover = createDto.coverUrl?.let { url ->
            if (url.isNotEmpty()) {
                val data = Parser.downloadCoverImage(url)
                if (data != null) {
                    val image = ThumbnailProcessor.getImage(data)
                    return@let image
                }
            }
            return@let null
        }

        return transaction {
            AlbumEntity.new {
                this.title = createDto.title!!
                this.description = createDto.description ?: ""
                this.releaseYear = createDto.year
                this.single = createDto.isSingle
                this.cover = cover
            }
        }
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

    fun getByArtistId(artistId: Long, userId: Long? = null): Map<AlbumEntity, List<TrackEntity>> = transaction {
        val albumIds = TrackTable.select(TrackTable.albumId).where {
            (TrackTable.id inSubQuery (
                TrackArtistConnection
                    .select(TrackArtistConnection.track)
                    .where { TrackArtistConnection.artist eq artistId }
            ))
        }.map { it[TrackTable.albumId] }

        return@transaction AlbumEntity.find { (notBlockedByUserOp(userId)) and (AlbumTable.id inList albumIds) }
            .associateWith { album ->
                TrackRepo
                    .getAllFromAlbum(album.id.value, userId)
                    .filter { it.artists.any { artist -> artist.id.value == artistId } }
            }
    }

    suspend fun update(albumId: Long, editDto: AlbumEditDto): AlbumEntity? {
        val album = getById(albumId) ?: return null

        val (update, newCover) = ImageRepo.getUpdatedImage(editDto.coverUrl)

        return transaction {
            editDto.title?.takeIf { it.isNotBlank() }?.let { album.title = it }
            editDto.description?.let { album.description = it }
            album.releaseYear = editDto.year
            album.single = editDto.isSingle
            if (update) {
                album.cover = newCover
            }
            album
        }
    }

    fun notSingleOp(): Op<Boolean> {
        return (AlbumTable.single eq false) or
                (AlbumTable.id inSubQuery TrackTable
                    .select(TrackTable.albumId)
                    .groupBy(TrackTable.albumId)
                    .having { Count(TrackTable.id) greater 1L }
                )
    }

    fun estimateIsSingle(albumId: Long): Boolean = transaction {
        val trackCounts = TrackTable.select(TrackTable.id).where {
            (TrackTable.albumId eq albumId)
        }.count()
        return@transaction trackCounts <= 1
    }

    fun delete(albumId: Long): Boolean = transaction {

        val album = AlbumEntity.findById(albumId) ?: return@transaction false
        album.delete()

        return@transaction true
    }

    fun getUnusedAlbums(): Pair<SizedIterable<AlbumEntity>, Long> = transaction {
        val unusedAlbums = AlbumEntity.find {
            AlbumTable.id notInSubQuery (TrackTable.select(TrackTable.albumId))
        }
        val count = unusedAlbums.count()
        return@transaction unusedAlbums to count
    }

    fun deleteAll(albums: SizedIterable<AlbumEntity>) = transaction {
        albums.forEach { it.delete() }
    }

    fun estimateReleaseYear(albumId: Long): Int? = transaction {
        val years = TrackTable.select(TrackTable.year).where {
            (TrackTable.albumId eq albumId) and (TrackTable.year neq null)
        }.mapNotNull { it[TrackTable.year] }
        return@transaction years.maxOrNull()
    }

    private fun notBlockedByUserOp(userId: Long? = null): Op<Boolean> {

        if (userId == null) {
            return Op.TRUE
        }

        return notBlockedByTagsOp(userId) and notBlockedByArtistsOp(userId)
    }

    private fun notBlockedByTagsOp(userId: Long? = null): Op<Boolean> {

        if (userId == null) {
            return Op.TRUE
        }

        val blockedTagIds = Settings.get(BlockedTags, userId)
        if (blockedTagIds.isEmpty()) {
            return Op.TRUE
        }

        return AlbumTable.id notInSubQuery (
            TrackTable
                .select(TrackTable.albumId)
                .where {
                    TrackTable.id inSubQuery (
                        TrackTagConnection
                            .select(TrackTagConnection.track)
                            .where { TrackTagConnection.tag inList blockedTagIds }
                    )
                }
        )
    }

    private fun notBlockedByArtistsOp(userId: Long? = null): Op<Boolean> {

        if (userId == null) {
            return Op.TRUE
        }

        val blockedArtistIds = Settings.get(BlockedArtists, userId)
        if (blockedArtistIds.isEmpty()) {
            return Op.TRUE
        }

        return AlbumTable.id notInSubQuery (
            TrackTable
                .select(TrackTable.albumId)
                .where {
                    TrackTable.id inSubQuery (
                        TrackArtistConnection
                            .select(TrackArtistConnection.track)
                            .where { TrackArtistConnection.artist inList blockedArtistIds }
                    )
                }
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

    fun toDto(albumEntity: AlbumEntity): AlbumDto = transaction {
        return@transaction toDtoInternal(albumEntity)
    }

    fun toDto(albumEntities: List<AlbumEntity>): List<AlbumDto> = transaction {
        return@transaction albumEntities.map { toDtoInternal(it) }
    }

    fun toDataDto(albumEntity: AlbumEntity, userId: Long? = null): AlbumDataDto = transaction {
        return@transaction AlbumDataDto(
            album = toDtoInternal(albumEntity),
            tracks = TrackRepo.toDto(TrackRepo.getAllFromAlbum(albumEntity.id.value, userId))
        )
    }

    internal fun toDtoInternal(albumEntity: AlbumEntity): AlbumDto {
        return AlbumDto(
            id = albumEntity.id.value,
            title = albumEntity.title,
            description = albumEntity.description,
            artists = getArtistsForAlbum(albumEntity).map { ArtistRepo.toDtoInternal(it) },
            trackCount = getSongAmountForAlbum(albumEntity),
            year = albumEntity.releaseYear ?: estimateReleaseYear(albumEntity.id.value),
            single = albumEntity.single ?: estimateIsSingle(albumEntity.id.value),
            createdAt = albumEntity.createdAt,
            updatedAt = albumEntity.updatedAt
        )
    }
}