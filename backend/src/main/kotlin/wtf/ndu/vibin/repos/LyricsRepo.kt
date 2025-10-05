package wtf.ndu.vibin.repos

import org.jetbrains.exposed.sql.transactions.transaction
import wtf.ndu.vibin.db.LyricsEntity
import wtf.ndu.vibin.db.LyricsTable
import wtf.ndu.vibin.db.tracks.TrackEntity

object LyricsRepo {

    fun setLyrics(track: TrackEntity, lyrics: String?): LyricsEntity? = transaction {
        if (lyrics == null) {
            deleteLyrics(track)
            return@transaction null
        }

        val existing = LyricsEntity.find { LyricsTable.track eq track.id }.firstOrNull()
        if (existing != null) {
            existing.content = lyrics
            return@transaction existing
        }

        return@transaction LyricsEntity.new {
            this.track = track
            this.content = lyrics
        }
    }

    fun getLyrics(track: TrackEntity): LyricsEntity? = transaction {
        return@transaction LyricsEntity.find { (LyricsTable.track eq track.id) }.firstOrNull()
    }

    fun deleteLyrics(track: TrackEntity) = transaction {
        LyricsEntity.find { LyricsTable.track eq track.id }.forEach { it.delete() }
    }

    fun hasLyrics(trackId: Long): Boolean = transaction {
        return@transaction !LyricsEntity.find { (LyricsTable.track eq trackId) }.empty()
    }
}