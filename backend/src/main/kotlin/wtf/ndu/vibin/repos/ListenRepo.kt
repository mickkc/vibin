package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.ListenEntity
import wtf.ndu.vibin.db.ListenTable
import wtf.ndu.vibin.db.artists.ArtistEntity
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.db.tracks.TrackEntity
import wtf.ndu.vibin.utils.DateTimeUtils

object ListenRepo {

    fun listenedTo(userId: Long, trackId: Long) = transaction {
        val now = DateTimeUtils.now()
        val lastListenForUser = ListenEntity
            .find { ListenTable.user eq userId }
            .orderBy(ListenTable.listenedAt to SortOrder.DESC)
            .firstOrNull()
        if (lastListenForUser != null && lastListenForUser.track.id.value == trackId) {
            val lastTrack = lastListenForUser.track
            val difference = now - lastListenForUser.listenedAt
            if (lastTrack.duration == null || difference * 1000 <= lastTrack.duration!!) {
                // If the last listened track is the same and the difference is less than the track duration, do not log a new listen
                return@transaction
            }
        }

        val user = UserRepo.getById(userId) ?: return@transaction
        val track = TrackRepo.getById(trackId) ?: return@transaction

        ListenEntity.new {
            this.user = user
            this.track = track
            this.listenedAt = now
        }
    }

    fun getMostListenedTracks(userId: Long, since: Long): Map<TrackEntity, Int> = transaction {
        ListenEntity
            .find { (ListenTable.listenedAt greaterEq since) and (ListenTable.user eq userId) }
            .groupBy { it.track }
            .mapValues { it.value.size }
    }

    fun getMostListenedArtists(tracks: Map<TrackEntity, Int>): Map<ArtistEntity, Int> = transaction {
        val artistCount = mutableMapOf<ArtistEntity, Int>()
        tracks.forEach { (track, count) ->
            track.artists.forEach { artist ->
                artistCount[artist] = artistCount.getOrDefault(artist, 0) + count
            }
        }
        return@transaction artistCount
    }

    fun getMostListenedTags(tracks: Map<TrackEntity, Int>): Map<TagEntity, Int> = transaction {
        val tagCount = mutableMapOf<TagEntity, Int>()
        tracks.forEach { (track, count) ->
            track.tags.forEach { tag ->
                tagCount[tag] = tagCount.getOrDefault(tag, 0) + count
            }
        }
        return@transaction tagCount
    }
}