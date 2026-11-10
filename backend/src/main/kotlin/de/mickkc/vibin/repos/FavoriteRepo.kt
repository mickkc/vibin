package de.mickkc.vibin.repos

import de.mickkc.vibin.db.FavoriteTable
import de.mickkc.vibin.db.FavoriteType
import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackVisibility
import de.mickkc.vibin.dto.FavoriteDto
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.collections.map

object FavoriteRepo {

    fun getFavoriteDtoForUser(userId: Long, requestingUserId: Long?): FavoriteDto = transaction {
        FavoriteDto(
            tracks = getFavoriteTracksForUser(userId, requestingUserId).map { it?.let { TrackRepo.toMinimalDtoInternal(it) } },
            albums = getFavoriteAlbumsForUser(userId, requestingUserId).map { it?.let { AlbumRepo.toDtoInternal(it) } },
            artists = getFavoriteArtistsForUser(userId, requestingUserId).map { it?.let { ArtistRepo.toDtoInternal(it) } },
        )
    }

    fun getFavoriteTracksForUser(userId: Long, requestingUserId: Long?): List<TrackEntity?> = transaction {
        val trackIds = getFavoriteIdsByTypeForUser(userId, FavoriteType.TRACK)
        return@transaction (1..3).map { place ->
            val pair = trackIds.firstOrNull { it.second == place }
            pair?.let { TrackEntity.findById(it.first) }?.takeIf { requestingUserId != null || it.visibility == TrackVisibility.Public }
        }
    }

    fun getFavoriteAlbumsForUser(userId: Long, requestingUserId: Long?): List<AlbumEntity?> = transaction {
        val albumIds = getFavoriteIdsByTypeForUser(userId, FavoriteType.ALBUM)
        return@transaction (1..3).map { place ->
            val pair = albumIds.firstOrNull { it.second == place }
            pair?.let { AlbumEntity.findById(it.first) }?.takeIf {
                requestingUserId != null || TrackRepo.getAllFromAlbum(it.id.value, requestingUserId).let { tracks ->
                    tracks.isEmpty() || tracks.any { track -> track.visibility == TrackVisibility.Public }
                }
            }
        }
    }

    fun getFavoriteArtistsForUser(userId: Long, requestingUserId: Long?): List<ArtistEntity?> = transaction {
        val artistIds = getFavoriteIdsByTypeForUser(userId, FavoriteType.ARTIST)
        return@transaction (1..3).map { place ->
            val pair = artistIds.firstOrNull { it.second == place }
            pair?.let { ArtistEntity.findById(it.first) }?.takeIf {
                requestingUserId != null || TrackRepo.getByArtistId(it.id.value, requestingUserId).let { tracks ->
                    tracks.isEmpty() || tracks.any { track -> track.visibility == TrackVisibility.Public }
                }
            }
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