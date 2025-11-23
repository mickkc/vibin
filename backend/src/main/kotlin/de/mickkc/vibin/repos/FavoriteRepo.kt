package de.mickkc.vibin.repos

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction
import de.mickkc.vibin.db.FavoriteTable
import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.dto.FavoriteDto
import kotlin.collections.map

object FavoriteRepo {

    fun getFavoriteDtoForUser(userId: Long): FavoriteDto = transaction {
        FavoriteDto(
            tracks = getFavoriteTracksForUser(userId).map { it?.let { TrackRepo.toMinimalDtoInternal(it) } },
            albums = getFavoriteAlbumsForUser(userId).map { it?.let { AlbumRepo.toDtoInternal(it) } },
            artists = getFavoriteArtistsForUser(userId).map { it?.let { ArtistRepo.toDtoInternal(it) } },
        )
    }

    internal fun getFavoriteTracksForUser(userId: Long): List<TrackEntity?> {
        val trackIds = getFavoriteIdsByTypeForUser(userId, FavoriteType.TRACK)
        return (1..3).map { place ->
            val pair = trackIds.firstOrNull { it.second == place }
            pair?.let { TrackEntity.findById(it.first) }
        }
    }

    internal fun getFavoriteAlbumsForUser(userId: Long): List<AlbumEntity?> {
        val albumIds = getFavoriteIdsByTypeForUser(userId, FavoriteType.ALBUM)
        return (1..3).map { place ->
            val pair = albumIds.firstOrNull { it.second == place }
            pair?.let { AlbumEntity.findById(it.first) }
        }
    }

    internal fun getFavoriteArtistsForUser(userId: Long): List<ArtistEntity?> {
        val artistIds = getFavoriteIdsByTypeForUser(userId, FavoriteType.ARTIST)
        return (1..3).map { place ->
            val pair = artistIds.firstOrNull { it.second == place }
            pair?.let { ArtistEntity.findById(it.first) }
        }
    }

    internal fun getFavoriteIdsByTypeForUser(userId: Long, type: FavoriteType): List<Pair<Long, Int>> {
        return FavoriteTable.select(FavoriteTable.entityId, FavoriteTable.place).where {
            (FavoriteTable.userId eq userId) and (FavoriteTable.entityType eq type)
        }.orderBy(FavoriteTable.place to SortOrder.ASC).map { it[FavoriteTable.entityId] to it[FavoriteTable.place] }
    }

    fun addFavorite(userId: Long, entityType: FavoriteType, entityId: Long, place: Int) = transaction {

        // Remove existing favorite with same entityId or place
        FavoriteTable.deleteWhere {
            (FavoriteTable.userId eq userId) and
            (FavoriteTable.entityType eq entityType) and
            ((FavoriteTable.entityId eq entityId) or (FavoriteTable.place eq place))
        }

        // Insert new favorite
        FavoriteTable.insert {
            it[FavoriteTable.userId] = userId
            it[FavoriteTable.entityType] = entityType
            it[FavoriteTable.entityId] = entityId
            it[FavoriteTable.place] = place
        }
    }

    fun deleteFavoriteAtPlace(userId: Long, entityType: FavoriteType, place: Int) = transaction {
        FavoriteTable.deleteWhere {
            (FavoriteTable.userId eq userId) and
            (FavoriteTable.entityType eq entityType) and
            (FavoriteTable.place eq place)
        }
    }

    fun getPlace(userId: Long, entityType: FavoriteType, entityId: Long): Int? = transaction {
        FavoriteTable.select(FavoriteTable.place).where {
            (FavoriteTable.userId eq userId) and
            (FavoriteTable.entityType eq entityType) and
            (FavoriteTable.entityId eq entityId)
        }.map { it[FavoriteTable.place] }.firstOrNull()
    }
}