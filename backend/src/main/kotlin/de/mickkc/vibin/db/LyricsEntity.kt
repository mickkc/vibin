package de.mickkc.vibin.db

import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import de.mickkc.vibin.db.tracks.TrackEntity
import de.mickkc.vibin.db.tracks.TrackTable

object LyricsTable : ModifiableLongIdTable("lyrics") {
    val track = reference("track_id", TrackTable).uniqueIndex()
    val content = text("content")
}

/**
 * Lyrics entity representing the lyrics associated with a track.
 *
 * @property track The track associated with these lyrics.
 * @property content The content of the lyrics (LRC format or plain text).
 */
class LyricsEntity(id: EntityID<Long>) : ModifiableLongIdEntity(id, LyricsTable) {
    companion object : LongEntityClass<LyricsEntity>(LyricsTable)

    var track by TrackEntity referencedOn LyricsTable.track
    var content by LyricsTable.content
}