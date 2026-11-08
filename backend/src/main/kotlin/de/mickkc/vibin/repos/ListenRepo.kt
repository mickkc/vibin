package de.mickkc.vibin.repos

import de.mickkc.vibin.db.ListenEntity
import de.mickkc.vibin.db.ListenTable
import de.mickkc.vibin.db.ListenType
import de.mickkc.vibin.db.albums.AlbumEntity
import de.mickkc.vibin.db.albums.AlbumTable
import de.mickkc.vibin.db.artists.ArtistEntity
import de.mickkc.vibin.db.playlists.PlaylistEntity
import de.mickkc.vibin.db.tags.TagEntity
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable
import de.mickkc.vibin.db.tracks.TrackVisibility
import de.mickkc.vibin.dto.KeyValueDto
import de.mickkc.vibin.utils.DateTimeUtils
import de.mickkc.vibin.utils.UserActivity
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inSubQuery
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

object ListenRepo {

    fun listenedTo(userId: Long, entityId: Long, type: ListenType, at: Long = DateTimeUtils.now()): Boolean = transaction {

        if (type == ListenType.TRACK) {
            val lastListenForUser = ListenEntity
                .find { (ListenTable.user eq userId) and (ListenTable.type eq ListenType.TRACK) }
                .orderBy(ListenTable.listenedAt to SortOrder.DESC)
                .firstOrNull()
            if (lastListenForUser != null && lastListenForUser.entityId == entityId) {
                val lastTrack = TrackRepo.getById(entityId) ?: return@transaction false
                val difference = at - lastListenForUser.listenedAt
                if (lastTrack.duration != null && difference * 1000 <= lastTrack.duration!!) {
                    // If the last listened track is the same and the difference is less than the track duration, do not log a new listen
                    return@transaction false
                }
            }
        }

        val user = UserRepo.getById(userId) ?: return@transaction false

        ListenEntity.new {
            this.user = user
            this.entityId = entityId
            this.type = type
            this.listenedAt = at
        }

        return@transaction true
    }

    private fun createOp(userId: Long?): Op<Boolean> {
        return if (userId != null) {
            ListenTable.user eq userId
        } else {
            Op.TRUE
        }
    }

    fun getMostListenedTracks(userId: Long?, since: Long, requestingUserId: Long?): Map<TrackEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and createOp(userId) and (ListenTable.type eq ListenType.TRACK) and listenTrackVisibleToUser(requestingUserId) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { TrackRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedAlbums(userId: Long?, since: Long, requestingUserId: Long?): Map<AlbumEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and createOp(userId) and (ListenTable.type eq ListenType.ALBUM) and listenAlbumVisibleToUser(requestingUserId) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { AlbumRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedArtistsByTracks(userId: Long?, since: Long, requestingUserId: Long?): Map<ArtistEntity, Int> = transaction {

        val mostListenedTracks = getMostListenedTracks(userId, since, requestingUserId)

        val artists = mostListenedTracks.flatMap { it.key.artists }

        return@transaction artists.groupingBy { it }.eachCount()
    }

    fun getMostListenedArtists(userId: Long?, since: Long): Map<ArtistEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and createOp(userId) and (ListenTable.type eq ListenType.ARTIST) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { ArtistRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedPlaylists(userId: Long?, since: Long): Map<PlaylistEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and createOp(userId) and (ListenTable.type eq ListenType.PLAYLIST) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys {
                if (userId == null)
                    PlaylistRepo.getByIdPublic(it.key)
                else
                    PlaylistRepo.getById(it.key, userId)
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedToAsDtos(
        userId: Long,
        since: Long,
        requestingUserId: Long?,
        global: Boolean = false,
        allowedTypes: List<ListenType> = PermissionRepo.getPermittedListenTypes(userId) - ListenType.TRACK
    ): Map<KeyValueDto, Int> = transaction {

        val dtos = mutableMapOf<KeyValueDto, Int>()

        if (allowedTypes.contains(ListenType.TRACK)) {
            dtos += getMostListenedTracks(userId.takeIf { !global }, since, requestingUserId).mapKeys {
                KeyValueDto(ListenType.TRACK.name, TrackRepo.toDto(it.key))
            }
        }

        if (allowedTypes.contains(ListenType.ALBUM)) {
            dtos += getMostListenedAlbums(userId.takeIf { !global }, since, requestingUserId).mapKeys {
                KeyValueDto(ListenType.ALBUM.name, AlbumRepo.toDto(it.key))
            }
        }

        if (allowedTypes.contains(ListenType.ARTIST)) {
            dtos += getMostListenedArtists(userId.takeIf { !global }, since).mapKeys {
                KeyValueDto(ListenType.ARTIST.name, ArtistRepo.toDto(it.key))
            }
        }

        if (allowedTypes.contains(ListenType.PLAYLIST)) {
            dtos += getMostListenedPlaylists(userId.takeIf { !global }, since).mapKeys {
                KeyValueDto(ListenType.PLAYLIST.name, PlaylistRepo.toDto(it.key))
            }
        }

        return@transaction dtos
    }

    fun getMostListenedTags(userId: Long, since: Long, requestingUserId: Long?): Map<TagEntity, Int> = transaction {
        val tracks = getMostListenedTracks(userId, since, requestingUserId)
        val tagCount = mutableMapOf<TagEntity, Int>()
        tracks.forEach { (track, count) ->
            track.tags.forEach { tag ->
                tagCount[tag] = tagCount.getOrDefault(tag, 0) + count
            }
        }
        return@transaction tagCount
    }

    /**
     * Filters out track-listens to only include tracks the user is allowed to see.
     * If [userId] is null, only public tracks will be included, otherwise, instance-only tracks are included as well.
     */
    private fun listenTrackVisibleToUser(userId: Long?): Op<Boolean> {
        if (userId != null) return Op.TRUE
        return (ListenTable.entityId inSubQuery TrackTable.select(TrackTable.id).where {
            TrackTable.visibility eq TrackVisibility.Public
        })
    }

    /**
     * Gets the most recently listened to tracks, sorted so the most recent tracks come first.
     *
     * @param userId The user to get the most recently listened tracks for.
     * @param since The starting timestamp. Tracks listened to before it will not be included.
     * @param limit The maximum amount of tracks to return.
     * @param requestingUserId If null, only public tracks will be returned.
     */
    fun getRecentTracks(userId: Long, since: Long, limit: Int, requestingUserId: Long?): List<TrackEntity> = transaction {
        return@transaction ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.TRACK) and listenTrackVisibleToUser(requestingUserId) }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .map { it.entityId }
            .distinct()
            .take(limit)
            .mapNotNull { TrackRepo.getById(it) }
    }

    /**
     * Filters out listens of albums that only contain instance-only tracks.
     * If [userId] is null, only albums containing at least one public track are included, otherwise, instance-only
     * albums are included as well.
     */
    private fun listenAlbumVisibleToUser(userId: Long?): Op<Boolean> {
        if (userId != null) return Op.TRUE
        return (ListenTable.entityId inSubQuery AlbumTable.select(TrackTable.id).where {
            AlbumTable.id inSubQuery TrackTable.select(TrackTable.albumId).where {
                TrackTable.visibility eq TrackVisibility.Public
            }
        })
    }

    /**
     * Gets the most recently listened to albums, sorted so the most recent albums come first.
     *
     * @param userId The user to get the most recently listened albums for.
     * @param since The starting timestamp. Albums listened to before it will not be included.
     * @param limit The maximum amount of albums to return.
     * @param requestingUserId If null, only albums with at least one public track will be returned.
     */
    fun getRecentAlbums(userId: Long, since: Long, limit: Int, requestingUserId: Long?): List<AlbumEntity> = transaction {
        return@transaction ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.ALBUM) and listenAlbumVisibleToUser(requestingUserId) }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .map { it.entityId }
            .distinct()
            .take(limit)
            .mapNotNull { AlbumRepo.getById(it) }
    }

    /**
     * Gets the most recently listened to artists based on listened to tracks, sorted so the most recent artists come first.
     *
     * @param userId The user to get the most recently listened artists for.
     * @param since The starting timestamp. Artists listened to before it will not be included.
     * @param limit The maximum amount of artists to return.
     * @param requestingUserId If null, only artists from public tracks will be returned.
     */
    fun getRecentArtistsFromTracks(userId: Long, since: Long, limit: Int, requestingUserId: Long?): List<ArtistEntity> = transaction {
        return@transaction ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.TRACK) and listenTrackVisibleToUser(requestingUserId) }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .asSequence()
            .map { it.entityId }
            .distinct()
            .take(limit)
            .flatMap { TrackRepo.getById(it)?.let { TrackRepo.getArtists(it) } ?: emptyList() }
            .distinct()
            .toList()
    }

    fun getRecentNonTrackDtos(
        userId: Long,
        limit: Int,
        requestingUserId: Long?,
        allowedTypes: List<ListenType> = PermissionRepo.getPermittedListenTypes(userId) - ListenType.TRACK
    ): List<KeyValueDto> = transaction {

        return@transaction ListenEntity
            .find { (ListenTable.user eq userId) and (ListenTable.type inList allowedTypes) and
                ((ListenTable.type neq ListenType.TRACK) or listenTrackVisibleToUser(requestingUserId)) and
                ((ListenTable.type neq ListenType.ALBUM) or listenAlbumVisibleToUser(requestingUserId))
            }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .mapNotNull { KeyValueDto(it.type.name, when (it.type) {
                ListenType.ALBUM -> AlbumRepo.toDtoInternal(AlbumRepo.getById(it.entityId) ?: return@mapNotNull null)
                ListenType.ARTIST -> ArtistRepo.toDtoInternal(ArtistRepo.getById(it.entityId) ?: return@mapNotNull null)
                ListenType.PLAYLIST -> PlaylistRepo.toDtoInternal(PlaylistRepo.getById(it.entityId, userId) ?: return@mapNotNull null)
                ListenType.TRACK -> TrackRepo.toDtoInternal(TrackRepo.getById(it.entityId) ?: return@mapNotNull null)
                else -> null
            } ?: return@mapNotNull null) }
            .distinct()
            .take(limit)
    }

    fun getActivityForUser(userId: Long, since: Long, limit: Int, requestingUserId: Long): UserActivity {
        val recentTracks = getRecentTracks(userId, since, limit, requestingUserId)

        val topTracks = getMostListenedTracks(userId, since, requestingUserId)
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        val topArtists = getMostListenedArtistsByTracks(userId, since, requestingUserId)
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        return UserActivity(
            recentTracks = recentTracks,
            topTracks = topTracks,
            topArtists = topArtists
        )
    }

    fun getTotalListenedTracks(): Long = transaction {
        return@transaction ListenEntity
            .find { ListenTable.type eq ListenType.TRACK }
            .count()
    }
}