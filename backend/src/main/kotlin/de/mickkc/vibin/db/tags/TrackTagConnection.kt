package de.mickkc.vibin.db.tags

import org.jetbrains.exposed.sql.Table
import de.mickkc.vibin.db.tracks.TrackTable

/**
 * Connection table between tracks and tags.
 *
 * @property track Reference to the track.
 * @property tag Reference to the tag.
 * @primaryKey Composite primary key consisting of track and tag.
 */
object TrackTagConnection : Table("track_tag") {
    val track = reference("track_id", TrackTable)
    val tag = reference("tag_id", TagTable)

    override val primaryKey = PrimaryKey(track, tag, name = "PK_TrackTag")
}