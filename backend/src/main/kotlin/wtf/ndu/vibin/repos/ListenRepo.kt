package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.ListenEntity
import wtf.ndu.vibin.db.ListenTable
import wtf.ndu.vibin.db.ListenType
import wtf.ndu.vibin.db.albums.AlbumEntity
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.playlists.PlaylistEntity
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.dto.KeyValueDto
import wtf.ndu.vibin.utils.DateTimeUtils
import wtf.ndu.vibin.utils.UserActivity

object ListenRepo {

    fun listenedTo(userId: Long, entityId: Long, type: ListenType): Boolean = transaction {
        val now = DateTimeUtils.now()

        if (type == ListenType.TRACK) {
            val lastListenForUser = ListenEntity
                .find { (ListenTable.user eq userId) and (ListenTable.type eq ListenType.TRACK) }
                .orderBy(ListenTable.listenedAt to SortOrder.DESC)
                .firstOrNull()
            if (lastListenForUser != null && lastListenForUser.entityId == entityId) {
                val lastTrack = TrackRepo.getById(entityId) ?: return@transaction false
                val difference = now - lastListenForUser.listenedAt
                if (lastTrack.duration == null || difference * 1000 <= lastTrack.duration!!) {
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
            this.listenedAt = now
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

    fun getMostListenedTracks(userId: Long?, since: Long): Map<TrackEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and createOp(userId) and (ListenTable.type eq ListenType.TRACK) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { TrackRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedAlbums(userId: Long?, since: Long): Map<AlbumEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and createOp(userId) and (ListenTable.type eq ListenType.ALBUM) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { AlbumRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedArtistsByTracks(userId: Long?, since: Long): Map<ArtistEntity, Int> = transaction {

        val mostListenedTracks = getMostListenedTracks(userId, since)

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

    fun getMostListenedToAsDtos(userId: Long, since: Long, global: Boolean = false): Map<KeyValueDto, Int> = transaction {

        val allowedTypes = PermissionRepo.getPermittedListenTypes(userId) - ListenType.TRACK

        val dtos = mutableMapOf<KeyValueDto, Int>()

        if (allowedTypes.contains(ListenType.ALBUM)) {
            dtos += getMostListenedAlbums(userId.takeIf { !global }, since).mapKeys {
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

    fun getMostListenedTags(userId: Long, since: Long): Map<TagEntity, Int> = transaction {
        val tracks = getMostListenedTracks(userId, since)
        val tagCount = mutableMapOf<TagEntity, Int>()
        tracks.forEach { (track, count) ->
            track.tags.forEach { tag ->
                tagCount[tag] = tagCount.getOrDefault(tag, 0) + count
            }
        }
        return@transaction tagCount
    }

    fun getRecentTracks(userId: Long, limit: Int): List<TrackEntity> = transaction {
        return@transaction ListenEntity
            .find { (ListenTable.user eq userId) and (ListenTable.type eq ListenType.TRACK) }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .map { it.entityId }
            .distinct()
            .take(limit)
            .mapNotNull { TrackRepo.getById(it) }
    }

    fun getRecentNonTrackDtos(userId: Long, limit: Int): List<KeyValueDto> = transaction {

        val allowedTypes = PermissionRepo.getPermittedListenTypes(userId) - ListenType.TRACK

        return@transaction ListenEntity
            .find { (ListenTable.user eq userId) and (ListenTable.type inList allowedTypes) }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .mapNotNull { KeyValueDto(it.type.name, when (it.type) {
                ListenType.ALBUM -> AlbumRepo.toDto(AlbumRepo.getById(it.entityId) ?: return@mapNotNull null)
                ListenType.ARTIST -> ArtistRepo.toDto(ArtistRepo.getById(it.entityId) ?: return@mapNotNull null)
                ListenType.PLAYLIST -> PlaylistRepo.toDto(PlaylistRepo.getById(it.entityId, userId) ?: return@mapNotNull null)
                else -> null
            } ?: return@mapNotNull null) }
            .distinct()
            .take(limit)
    }

    fun getActivityForUser(userId: Long, since: Long, limit: Int): UserActivity {
        val recentTracks = getRecentTracks(userId, limit)

        val topTracks = getMostListenedTracks(userId, since)
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }

        val topArtists = getMostListenedArtistsByTracks(userId, since)
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
}