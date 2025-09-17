package wtf.ndu.vibin.repos

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
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

    fun getMostListenedTracks(userId: Long, since: Long): Map<TrackEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.TRACK) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { TrackRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedAlbums(userId: Long, since: Long): Map<AlbumEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.ALBUM) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { AlbumRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedArtists(userId: Long, since: Long): Map<ArtistEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.ARTIST) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { ArtistRepo.getById(it.key) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedPlaylists(userId: Long, since: Long): Map<PlaylistEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) and (ListenTable.type eq ListenType.PLAYLIST) }
            .groupBy { it.entityId }
            .mapValues { it.value.size }
            .mapKeys { PlaylistRepo.getById(it.key, userId) }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
    }

    fun getMostListenedToAsDtos(userId: Long, since: Long): Map<KeyValueDto, Int> = transaction {
        val albumCounts = getMostListenedAlbums(userId, since).mapKeys { KeyValueDto(ListenType.ALBUM.name, AlbumRepo.toDto(it.key)) }
        val artistCounts = getMostListenedArtists(userId, since).mapKeys { KeyValueDto(ListenType.ARTIST.name, ArtistRepo.toDto(it.key)) }
        val playlistCounts = getMostListenedPlaylists(userId, since).mapKeys { KeyValueDto(ListenType.PLAYLIST.name, PlaylistRepo.toDto(it.key)) }

        return@transaction albumCounts + artistCounts + playlistCounts
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
}